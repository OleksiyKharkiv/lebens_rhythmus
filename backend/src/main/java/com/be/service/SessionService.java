package com.be.service;

import com.be.domain.entity.Group;
import com.be.domain.entity.RecurrenceDay;
import com.be.domain.entity.Session;
import com.be.domain.entity.Venue;
import com.be.domain.repository.GroupRepository;
import com.be.domain.repository.SessionRepository;
import com.be.domain.repository.VenueRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Session is a child of Group (LR-ADR-022, LR-067) — every method here
 * operates relative to a parent Group, there is no standalone
 * "create a Session with no Group" entry point.
 */
@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;
    private final GroupRepository groupRepository;
    private final VenueRepository venueRepository;

    @Transactional(readOnly = true)
    public List<Session> findByGroupId(Long groupId) {
        return sessionRepository.findByGroupIdOrderByStartDateTimeAsc(groupId);
    }

    @Transactional
    public Session addSession(Long groupId, LocalDateTime startDateTime, LocalDateTime endDateTime, Long venueId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group not found with id: " + groupId));

        Session.SessionBuilder session = Session.builder()
                .group(group)
                .startDateTime(startDateTime)
                .endDateTime(endDateTime);

        if (venueId != null) {
            session.venue(resolveVenue(venueId));
        }

        return sessionRepository.save(session.build());
    }

    @Transactional
    public Session updateSession(Long sessionId, LocalDateTime startDateTime, LocalDateTime endDateTime, Long venueId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("Session not found with id: " + sessionId));

        session.setStartDateTime(startDateTime);
        session.setEndDateTime(endDateTime);
        session.setVenue(venueId != null ? resolveVenue(venueId) : null);

        return sessionRepository.save(session);
    }

    @Transactional
    public void deleteSession(Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("Session not found with id: " + sessionId));
        sessionRepository.delete(session);
    }

    /**
     * Replaces every Session under a Group with a fresh set in one call —
     * matches the admin form shape (LR-074): "number of days" resubmits
     * the whole day-list each time, not incremental add/remove. Group's
     * own enrollments/capacity are untouched — Session rows carry
     * schedule only.
     *
     * Group.startDateTime/endDateTime are synced to the earliest/latest
     * session (LR-074) — those fields stay the "day 1 / only day" values
     * per LR-ADR-022, read directly by code that doesn't know about
     * Session at all (public workshop-detail page, WorkshopMapper's
     * GroupDTO). Without this, replacing the day-list would leave them
     * silently stale.
     */
    @Transactional
    public List<Session> replaceSessionsForGroup(Long groupId, List<SessionInput> inputs) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group not found with id: " + groupId));

        group.getSessions().clear();
        for (SessionInput input : inputs) {
            Session.SessionBuilder session = Session.builder()
                    .group(group)
                    .startDateTime(input.startDateTime())
                    .endDateTime(input.endDateTime());
            if (input.venueId() != null) {
                session.venue(resolveVenue(input.venueId()));
            }
            group.getSessions().add(session.build());
        }

        if (!inputs.isEmpty()) {
            group.setStartDateTime(inputs.stream().map(SessionInput::startDateTime).min(LocalDateTime::compareTo).orElseThrow());
            LocalDateTime lastEnd = inputs.stream()
                    .map(i -> i.endDateTime() != null ? i.endDateTime() : i.startDateTime())
                    .max(LocalDateTime::compareTo).orElseThrow();
            group.setEndDateTime(lastEnd);
        }

        groupRepository.save(group);
        return group.getSessions();
    }

    /**
     * Generates Session rows for a Course-linked Group from its own
     * persisted recurrence rule (LR-082, LR-ADR-023) — one Session per
     * matching weekday in [recurrenceStartDate, recurrenceEndDate],
     * using that weekday's own start time/duration (not one shared time
     * for every day). Reuses replaceSessionsForGroup, so this is also a
     * full clear+re-add (same trade-off already accepted for the manual
     * multi-day case, LR-074) and Group.startDateTime/endDateTime end up
     * synced to the actual first/last generated occurrence, which may be
     * narrower than the admin-set window (e.g. window Jan 1–31, first
     * Monday is Jan 5) — expected, not a bug.
     *
     * Explicit call, not a side effect of every Group save (LR-ADR-023
     * п.3) — the admin form only calls this when recurrence fields
     * actually changed.
     */
    @Transactional
    public List<Session> generateSessionsFromRecurrence(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Group not found with id: " + groupId));

        List<RecurrenceDay> pattern = group.getRecurrenceDays();
        LocalDate start = group.getRecurrenceStartDate();
        LocalDate end = group.getRecurrenceEndDate();
        if (pattern == null || pattern.isEmpty() || start == null || end == null) {
            throw new IllegalStateException(
                    "Group " + groupId + " has no recurrence pattern/window to generate Sessions from");
        }
        if (end.isBefore(start)) {
            throw new IllegalStateException("recurrenceEndDate is before recurrenceStartDate for group " + groupId);
        }

        Map<DayOfWeek, RecurrenceDay> byWeekday = pattern.stream()
                .collect(Collectors.toMap(RecurrenceDay::dayOfWeek, d -> d, (a, b) -> a));

        List<SessionInput> inputs = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            RecurrenceDay day = byWeekday.get(date.getDayOfWeek());
            if (day == null) continue;
            LocalDateTime sessionStart = LocalDateTime.of(date, day.startTime());
            LocalDateTime sessionEnd = sessionStart.plusMinutes(day.durationMinutes());
            inputs.add(new SessionInput(sessionStart, sessionEnd, null));
        }

        return replaceSessionsForGroup(groupId, inputs);
    }

    private Venue resolveVenue(Long venueId) {
        return venueRepository.findById(venueId)
                .orElseThrow(() -> new EntityNotFoundException("Venue not found with id: " + venueId));
    }

    public record SessionInput(LocalDateTime startDateTime, LocalDateTime endDateTime, Long venueId) {
    }
}

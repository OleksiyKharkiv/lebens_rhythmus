package com.be.service;

import com.be.domain.entity.Group;
import com.be.domain.entity.Session;
import com.be.domain.entity.Venue;
import com.be.domain.repository.GroupRepository;
import com.be.domain.repository.SessionRepository;
import com.be.domain.repository.VenueRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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

    private Venue resolveVenue(Long venueId) {
        return venueRepository.findById(venueId)
                .orElseThrow(() -> new EntityNotFoundException("Venue not found with id: " + venueId));
    }

    public record SessionInput(LocalDateTime startDateTime, LocalDateTime endDateTime, Long venueId) {
    }
}

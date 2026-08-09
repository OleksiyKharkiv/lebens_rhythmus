package com.be.service;

import com.be.domain.entity.Group;
import com.be.domain.entity.Session;
import com.be.domain.entity.Venue;
import com.be.domain.repository.GroupRepository;
import com.be.domain.repository.SessionRepository;
import com.be.domain.repository.VenueRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * LR-067 (LR-ADR-022) — Session is a child of Group, not a standalone
 * registration target: a 3-day workshop is ONE Group (one roster/
 * capacity) with three Session rows underneath it, not three separate
 * Groups each requiring its own enrollment.
 */
@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private GroupRepository groupRepository;
    @Mock
    private VenueRepository venueRepository;

    private SessionService service() {
        return new SessionService(sessionRepository, groupRepository, venueRepository);
    }

    @Test
    void replaceSessionsForGroup_multiDay_createsOneSessionPerDay_withOwnVenues() {
        Group group = Group.builder().id(1L).capacity(20).build();
        Venue hallA = Venue.builder().id(10L).name("Hall A").build();
        Venue hallB = Venue.builder().id(11L).name("Hall B").build();

        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(venueRepository.findById(10L)).thenReturn(Optional.of(hallA));
        when(venueRepository.findById(11L)).thenReturn(Optional.of(hallB));
        when(groupRepository.save(any(Group.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime day1 = LocalDateTime.of(2026, 3, 1, 10, 0);
        LocalDateTime day2 = LocalDateTime.of(2026, 3, 2, 10, 0);
        LocalDateTime day3 = LocalDateTime.of(2026, 3, 3, 10, 0);

        List<Session> result = service().replaceSessionsForGroup(1L, List.of(
                new SessionService.SessionInput(day1, day1.plusHours(4), 10L),
                new SessionService.SessionInput(day2, day2.plusHours(4), 11L),
                new SessionService.SessionInput(day3, day3.plusHours(4), 10L)
        ));

        assertThat(result).hasSize(3);
        assertThat(result).extracting(Session::getVenue)
                .containsExactly(hallA, hallB, hallA);
        assertThat(result).allSatisfy(s -> assertThat(s.getGroup()).isEqualTo(group));
        // Group's own roster/capacity is untouched by adding Session rows —
        // one registration covers the whole multi-day Group regardless of
        // how many days it has (the whole point of LR-ADR-022).
        assertThat(group.getCapacity()).isEqualTo(20);
        assertThat(group.getEnrollments()).isEmpty();
    }

    @Test
    void replaceSessionsForGroup_clearsPreviousSessionsBeforeAddingNew() {
        Group group = Group.builder().id(1L).capacity(10).build();
        Session stale = Session.builder().id(99L).group(group)
                .startDateTime(LocalDateTime.of(2020, 1, 1, 9, 0)).build();
        group.getSessions().add(stale);

        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupRepository.save(any(Group.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime newDay = LocalDateTime.of(2026, 4, 1, 9, 0);
        List<Session> result = service().replaceSessionsForGroup(1L, List.of(
                new SessionService.SessionInput(newDay, newDay.plusHours(2), null)
        ));

        assertThat(result).hasSize(1);
        assertThat(result).doesNotContain(stale);
        assertThat(result.get(0).getVenue()).isNull();
    }

    @Test
    void addSession_resolvesGroupAndVenue() {
        Group group = Group.builder().id(1L).build();
        Venue venue = Venue.builder().id(5L).name("Studio").build();

        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(venueRepository.findById(5L)).thenReturn(Optional.of(venue));
        when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime start = LocalDateTime.of(2026, 5, 1, 18, 0);
        Session created = service().addSession(1L, start, start.plusHours(2), 5L);

        assertThat(created.getGroup()).isEqualTo(group);
        assertThat(created.getVenue()).isEqualTo(venue);
        assertThat(created.getStartDateTime()).isEqualTo(start);
    }

    @Test
    void addSession_venueOptional_nullWhenNotProvided() {
        Group group = Group.builder().id(1L).build();
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime start = LocalDateTime.of(2026, 5, 1, 18, 0);
        Session created = service().addSession(1L, start, start.plusHours(2), null);

        assertThat(created.getVenue()).isNull();
    }
}

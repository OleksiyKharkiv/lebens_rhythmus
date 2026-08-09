package com.be;

import com.be.domain.entity.Group;
import com.be.domain.entity.Session;
import com.be.domain.entity.Venue;
import com.be.domain.repository.GroupRepository;
import com.be.domain.repository.SessionRepository;
import com.be.domain.repository.VenueRepository;
import com.be.service.SessionService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LR-067 (LR-ADR-022) — real-DB proof for
 * SessionService.replaceSessionsForGroup()'s clear+re-add semantics.
 * SessionServiceTest (Mockito) proves the method calls the right
 * repository methods with the right arguments; it can't prove Hibernate's
 * orphanRemoval actually DELETEs the old group_sessions rows rather than
 * just detaching them from the in-memory collection — that needs a real
 * flush against real Postgres, per architect-reviewer's finding on this
 * ticket.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class SessionIntegrationTest {

    @Autowired
    private SessionService sessionService;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @Transactional
    void replaceSessionsForGroup_secondCall_actuallyDeletesOldRowsInRealDb_notJustDetachesThem() {
        Group group = Group.builder()
                .titleDe("Mehrtägiger Workshop").titleEn("Multi-day workshop").titleUa("Багатоденний воркшоп")
                .capacity(15)
                .startDateTime(LocalDateTime.now().plusDays(1))
                .build();
        Group savedGroup = groupRepository.save(group);

        Venue hallA = venueRepository.save(Venue.builder().name("Halle A").build());
        Venue hallB = venueRepository.save(Venue.builder().name("Halle B").build());

        LocalDateTime day1 = LocalDateTime.of(2026, 6, 1, 10, 0);
        LocalDateTime day2 = LocalDateTime.of(2026, 6, 2, 10, 0);
        LocalDateTime day3 = LocalDateTime.of(2026, 6, 3, 10, 0);

        // First call: 3 days.
        List<Session> firstResult = sessionService.replaceSessionsForGroup(savedGroup.getId(), List.of(
                new SessionService.SessionInput(day1, day1.plusHours(4), hallA.getId()),
                new SessionService.SessionInput(day2, day2.plusHours(4), hallB.getId()),
                new SessionService.SessionInput(day3, day3.plusHours(4), hallA.getId())
        ));
        entityManager.flush();
        List<Long> firstRoundIds = firstResult.stream().map(Session::getId).toList();

        Long countAfterFirst = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM group_sessions WHERE group_id = ?", Long.class, savedGroup.getId());
        assertThat(countAfterFirst).isEqualTo(3);

        // Second call: replace with a single day — the whole point of this
        // test is confirming the 3 old rows are genuinely gone from the
        // real table, not just dropped from the Java-side list.
        LocalDateTime newDay = LocalDateTime.of(2026, 7, 1, 9, 0);
        List<Session> secondResult = sessionService.replaceSessionsForGroup(savedGroup.getId(), List.of(
                new SessionService.SessionInput(newDay, newDay.plusHours(2), null)
        ));
        entityManager.flush();
        entityManager.clear();

        Long countAfterSecond = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM group_sessions WHERE group_id = ?", Long.class, savedGroup.getId());
        assertThat(countAfterSecond).isEqualTo(1);

        // Confirm by primary key, not just count — the original 3 rows'
        // ids must be genuinely absent, not merely outnumbered.
        for (Long staleId : firstRoundIds) {
            assertThat(sessionRepository.findById(staleId)).isEmpty();
        }

        Group reloadedGroup = groupRepository.findById(savedGroup.getId()).orElseThrow();
        assertThat(reloadedGroup.getSessions()).hasSize(1);
        assertThat(reloadedGroup.getSessions().get(0).getVenue()).isNull();
        // Group's own roster/capacity untouched by two rounds of Session
        // churn — same invariant SessionServiceTest asserts at the unit
        // level, reconfirmed here against the real persisted row.
        assertThat(reloadedGroup.getCapacity()).isEqualTo(15);
        assertThat(reloadedGroup.getEnrollments()).isEmpty();

        assertThat(secondResult).hasSize(1);
    }
}

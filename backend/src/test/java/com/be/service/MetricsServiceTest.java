package com.be.service;

import com.be.domain.repository.EnrollmentRepository;
import com.be.domain.repository.GroupRepository;
import com.be.domain.repository.UserRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * M5 threshold table (Круглый стол #3, LR-015, 2026-08-05): 7d/<30% info,
 * 5d/<50% warning, 3d/<70% urgent, 1d/<90% critical. Each threshold is
 * strict "<" — a group sitting exactly ON a threshold (e.g. 90% full with
 * 1 day left) has cleared the bar, not failed it, and gets no alert at
 * that tier. Boundary values are the easiest place for an off-by-one to
 * hide, hence covering them explicitly rather than relying only on the
 * full integration suite (which has no test data shaped to hit them).
 */
@ExtendWith(MockitoExtension.class)
class MetricsServiceTest {

    @Mock
    private GroupRepository groupRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;

    private MetricsService service() {
        return new MetricsService(groupRepository, userRepository, enrollmentRepository);
    }

    @ParameterizedTest
    @CsvSource({
            // daysUntilStart, fillRatio, expected level ('' = no alert)
            "0,    0.10, critical",
            "1,    0.89, critical",
            "1,    0.90, ''",       // exactly at the bar — cleared, not critical, and 0.90 also fails every laxer tier's threshold
            "2,    0.10, urgent",   // outside critical's 1-day window, but still within urgent's
            "3,    0.69, urgent",
            "3,    0.70, ''",       // exactly at urgent's bar — and fails warning/info too
            "4,    0.10, warning",  // outside urgent's window, within warning's
            "5,    0.49, warning",
            "5,    0.50, ''",       // exactly at warning's bar — and fails info too
            "6,    0.10, info",     // outside warning's window, within info's
            "7,    0.29, info",
            "7,    0.30, ''",       // exactly at info's bar
            "8,    0.00, ''"        // outside every tier's day window entirely
    })
    void alertLevel_appliesTightestApplicableThreshold(long daysUntilStart, double fillRatio, String expected) {
        String level = service().alertLevel(daysUntilStart, fillRatio);
        assertThat(level).isEqualTo(expected.isEmpty() ? null : expected);
    }
}

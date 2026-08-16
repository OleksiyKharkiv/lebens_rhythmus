package com.be.service;

import com.be.domain.entity.Enrollment;
import com.be.domain.entity.User;
import com.be.domain.entity.enums.EnrollmentStatus;
import com.be.domain.repository.EnrollmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * LR-084 — 7-day TTL sweep for unpaid PENDING enrollments (round-2
 * roundtable, owner-confirmed 2026-08-16).
 */
@ExtendWith(MockitoExtension.class)
class EnrollmentCleanupServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private EnrollmentService enrollmentService;

    private EnrollmentCleanupService service() {
        return new EnrollmentCleanupService(enrollmentRepository, enrollmentService);
    }

    @Test
    void expireStalePendingEnrollments_callsExpireForEachStaleRow() {
        Enrollment stale1 = Enrollment.builder().id(1L).user(User.builder().id(1L).build())
                .status(EnrollmentStatus.PENDING).build();
        Enrollment stale2 = Enrollment.builder().id(2L).user(User.builder().id(2L).build())
                .status(EnrollmentStatus.PENDING).build();

        when(enrollmentRepository.findByStatusAndCreatedAtBefore(eq(EnrollmentStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(List.of(stale1, stale2));

        service().expireStalePendingEnrollments();

        verify(enrollmentService).expire(stale1);
        verify(enrollmentService).expire(stale2);
    }

    @Test
    void expireStalePendingEnrollments_oneFailure_doesNotBlockTheRest() {
        Enrollment bad = Enrollment.builder().id(1L).user(User.builder().id(1L).build())
                .status(EnrollmentStatus.PENDING).build();
        Enrollment ok = Enrollment.builder().id(2L).user(User.builder().id(2L).build())
                .status(EnrollmentStatus.PENDING).build();

        when(enrollmentRepository.findByStatusAndCreatedAtBefore(eq(EnrollmentStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(List.of(bad, ok));
        doThrow(new RuntimeException("boom")).when(enrollmentService).expire(bad);

        service().expireStalePendingEnrollments();

        verify(enrollmentService).expire(ok);
    }

    @Test
    void expireStalePendingEnrollments_usesSevenDayCutoff() {
        when(enrollmentRepository.findByStatusAndCreatedAtBefore(eq(EnrollmentStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(List.of());

        service().expireStalePendingEnrollments();

        ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(enrollmentRepository).findByStatusAndCreatedAtBefore(eq(EnrollmentStatus.PENDING), cutoffCaptor.capture());

        LocalDateTime expectedCutoff = LocalDateTime.now().minusDays(7);
        long diffSeconds = Math.abs(java.time.Duration.between(expectedCutoff, cutoffCaptor.getValue()).getSeconds());
        org.assertj.core.api.Assertions.assertThat(diffSeconds).isLessThan(5);
    }
}

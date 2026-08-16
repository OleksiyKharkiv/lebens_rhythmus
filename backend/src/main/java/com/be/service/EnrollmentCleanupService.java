package com.be.service;

import com.be.domain.entity.Enrollment;
import com.be.domain.entity.enums.EnrollmentStatus;
import com.be.domain.repository.EnrollmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

// LR-084 — 7-day TTL for unpaid PENDING enrollments (round-2 roundtable
// decision, owner-confirmed): a paid enrollment whose Order never got
// marked paid within 7 days auto-expires rather than sitting PENDING
// forever with no resolution path. First @Scheduled job in this project —
// see BackendApplication's @EnableScheduling.
@Service
public class EnrollmentCleanupService {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentCleanupService.class);
    private static final int TTL_DAYS = 7;

    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentService enrollmentService;

    public EnrollmentCleanupService(EnrollmentRepository enrollmentRepository, EnrollmentService enrollmentService) {
        this.enrollmentRepository = enrollmentRepository;
        this.enrollmentService = enrollmentService;
    }

    // 03:00 server time daily — low-traffic hour, matches this project's
    // general convention of avoiding maintenance work during real usage.
    @Scheduled(cron = "0 0 3 * * *")
    public void expireStalePendingEnrollments() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(TTL_DAYS);
        List<Enrollment> stale = enrollmentRepository.findByStatusAndCreatedAtBefore(EnrollmentStatus.PENDING, cutoff);

        for (Enrollment e : stale) {
            try {
                // Delegates to EnrollmentService.expire() — releases held
                // group capacity and sets EXPIRED, each in its own
                // transaction (EnrollmentService is class-level
                // @Transactional), so one bad row doesn't block the rest of
                // the batch.
                enrollmentService.expire(e);
            } catch (Exception ex) {
                log.error("Failed to expire stale enrollment id={}", e.getId(), ex);
            }
        }

        if (!stale.isEmpty()) {
            log.info("Expired {} stale PENDING enrollment(s) older than {} days", stale.size(), TTL_DAYS);
        }
    }
}

package com.be.domain.repository;

import com.be.domain.entity.Enrollment;
import com.be.domain.entity.enums.EnrollmentStatus;
import com.be.domain.entity.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    boolean existsByUserIdAndWorkshopId(Long userId, Long workshopId);

    // LR-084 — Course path, parallel to the Workshop check above (mirrors
    // uk_user_course, the DB-level twin of this app-level check).
    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    List<Enrollment> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Enrollment> findByWorkshopId(Long workshopId);

    List<Enrollment> findByCourseId(Long courseId);

    List<Enrollment> findByGroupId(Long groupId);

    // LR-084 — 7-day TTL cleanup job's query: unpaid (still PENDING) paid
    // enrollments whose Order never got confirmed within the window.
    List<Enrollment> findByStatusAndCreatedAtBefore(EnrollmentStatus status, LocalDateTime cutoff);

    // M6 (retention, LR-015) — one row per customer (Role.USER — same
    // restriction as M4's findByRoleAndCreatedAtAfter, since TEACHER/ADMIN/
    // BUSINESS_OWNER can also hold enrollments per EnrollmentController's
    // @PreAuthorize, and this metric is specifically about paying/enrolled
    // customers, not internal test enrollments) with at least one enrollment
    // in the given status, paired with how many distinct workshops that
    // covers. Row[0] = User.id, Row[1] = distinct workshop count.
    @Query("SELECT e.user.id, COUNT(DISTINCT e.workshop.id) FROM Enrollment e " +
            "WHERE e.status = :status AND e.user.role = :role GROUP BY e.user.id")
    List<Object[]> countDistinctWorkshopsPerUserWithStatus(@Param("status") EnrollmentStatus status,
                                                            @Param("role") Role role);
}
package com.be.domain.entity;

import com.be.domain.entity.enums.EnrollmentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// LR-084 — extended from Workshop-only to also support Course, and linked
// to Order for paid enrollments (both nullable, never both/neither set —
// same mutual-exclusivity discipline as Group.workshop/course, enforced in
// EnrollmentService, not the DB). uk_user_workshop alone stops covering
// duplicate registrations once workshop_id is nullable — Postgres treats
// NULL as distinct from NULL in a unique constraint, so a parallel
// uk_user_course is needed for the Course path, see V12 migration.
@Entity
@Table(name = "enrollments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_workshop", columnNames = {"user_id", "workshop_id"}),
        @UniqueConstraint(name = "uk_user_course", columnNames = {"user_id", "course_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // who enrolled
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    // which workshop — mutually exclusive with course, see class comment
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workshop_id")
    private Workshop workshop;

    // which course — mutually exclusive with workshop, see class comment
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    // which group (if any)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    // set only for paid enrollments (price > 0) — null for free ones, there's
    // nothing to pay so no Order is created. See EnrollmentService.enroll().
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EnrollmentStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = EnrollmentStatus.PENDING;
        }
    }
}
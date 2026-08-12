package com.be.domain.entity;

import com.be.config.RecurrenceDaysConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "workshop_groups",
        indexes = {
                @Index(name = "idx_group_teacher", columnList = "teacher_id"),
                @Index(name = "idx_group_start", columnList = "start_date_time"),
                @Index(name = "idx_group_active", columnList = "active")
        })
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // multilingual titles
    @Column(nullable = false)
    private String titleDe;

    @Column(nullable = false)
    private String titleEn;

    @Column(nullable = false)
    private String titleUa;

    // capacity for this group/session
    @Column(nullable = false)
    private int capacity; // max participants
    @Column(nullable = false)
    private int capacityLeft;

    // period / slot of this group (LocalDateTime per decision)
    @Column(name = "start_date_time", nullable = false)
    private LocalDateTime startDateTime;

    @Column(name = "end_date_time")
    private LocalDateTime endDateTime;

    // belongs to Activity 
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "activity_id", nullable = true)
    private Activity activity;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "age_group_id", nullable = true)
    private AgeGroup ageGroup;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "language_id", nullable = true)
    private Language language;

    // where this specific session happens — moved here from Workshop
    // (LR-015, 2026-08-05): a workshop's groups can run at different
    // times, each needing its own place.
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "venue_id", nullable = true)
    private Venue venue;

    // relationship to workshop
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "workshop_id")
    private Workshop workshop;

    // LR-081 (LR-ADR-023) — mutually exclusive with workshop above
    // (enforced in GroupService.createGroup, not the DB — same
    // unenforced-at-schema convention as Workshop.courseId/
    // Performance.courseId, LR-ADR-021). Only meaningful for
    // Course-linked groups; recurrencePattern/recurrenceStartDate/
    // recurrenceEndDate below stay null for Workshop-linked groups.
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    // Per-weekday time/duration for a Course-linked group's recurring
    // schedule — each selected day carries its own time, not one shared
    // time for every day. Generation window is recurrenceStartDate/
    // recurrenceEndDate below, deliberately separate from
    // startDateTime/endDateTime above, which keep meaning "actual
    // first/last occurrence" (synced from real Session rows once
    // generated, LR-067/LR-074) — the two pairs answer different
    // questions and would collide if merged.
    @Convert(converter = RecurrenceDaysConverter.class)
    @Column(name = "recurrence_pattern", columnDefinition = "TEXT")
    private List<RecurrenceDay> recurrenceDays;

    @Column(name = "recurrence_start_date")
    private LocalDate recurrenceStartDate;

    @Column(name = "recurrence_end_date")
    private LocalDate recurrenceEndDate;

    @OneToMany(fetch = LAZY)
    @JoinColumn(name = "participant_id")
    private Set<Participant> participants = new HashSet<>();

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    /**
     * Participant enrollments. Use Enrollment entity,
     * not Participant, because Enrollment stores status, date, etc.
     */
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Enrollment> enrollments = new HashSet<>();

    // Multi-day schedule (LR-ADR-022, LR-067) — additive to
    // startDateTime/endDateTime/venue above, which stay as the "day 1 /
    // only day" values for the common single-day case. A single roster
    // of enrollments/capacity above covers the whole Group regardless of
    // how many Session rows it has — deliberately not one enrollment per
    // day, see LR-ADR-022.
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Session> sessions = new ArrayList<>();

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    // ---- helper methods ----
    @Transient
    public int getEnrolledCount() {
        return enrollments == null ? 0 : enrollments.size();
    }

    @Transient
    public boolean isFull() {
        return getEnrolledCount() >= capacity;
    }

    @PrePersist
    protected void onCreate() {
        if (capacityLeft == 0) {
            capacityLeft = capacity;
        }
    }
}
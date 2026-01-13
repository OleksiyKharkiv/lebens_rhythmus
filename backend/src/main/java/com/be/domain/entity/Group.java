package com.be.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
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

    // multilingual titles (ok)
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

    // принадлежит Activity (ок)
    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "age_group_id", nullable = false)
    private AgeGroup ageGroup;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "language_id", nullable = false)
    private Language language;

    // связь к workshop (обязательно)
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "workshop_id")
    private Workshop workshop;

    @OneToMany(fetch = LAZY)
    @JoinColumn(name = "participant_id")
    private Set<Participant> participants = new HashSet<>();


    // teacher — рекомендую связать с User, если Teacher — это профиль в users.
    // Если у тебя есть отдельная Teacher entity, оставляем как есть.
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    /**
     * Записи участников. Рекомендую использовать Enrollment entity (см. ранее),
     * а не Participant, т.к. Enrollment хранит статус, дату и т.п.
     */
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Enrollment> enrollments = new HashSet<>();

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
}
package com.be.domain.entity;

import com.be.domain.entity.enums.WorkshopStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "workshops",
        indexes = {
                @Index(name = "idx_workshop_start", columnList = "start_date"),
                @Index(name = "idx_workshop_status", columnList = "status")
        })
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Workshop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "workshop_id")
    private Long id; // Long, не long

    @NotBlank
    @Size(max = 200)
    @Column(name = "workshop_name", nullable = false)
    private String workshopName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // Если время занятий не критичен — LocalDate; если важен — вернуть LocalDateTime
    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    /**
     * Общее ограничение участников (если применимо).
     * Но при наличии групп capacity лучше хранить на Group уровне.
     */
    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column(name = "price")
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private WorkshopStatus status;

    // Привязка к месту проведения
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id")
    private Venue venue;

    // Основной преподаватель (если требуется несколько — ManyToMany или отдельная сущность TeacherWorkshop)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private User teacher;

    // Если в ERM есть отдельная сущность GroupWorkshop — лучше её реализовать.
    // Здесь упрощенно: список групп (каждая группа содержит дату/время/capacity)
    @OneToMany(mappedBy = "workshop", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Group> groups = new ArrayList<>();

    // Перфомансы / представления (оставляем)
    @OneToMany(mappedBy = "workshop", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Performance> performances = new ArrayList<>();

    // Файлы — рекомендуем переименовать сущность в WorkshopFile/Attachment,
    // если есть риск конфликта с java.io.File
    @OneToMany(mappedBy = "workshop", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WorkshopFile> files = new ArrayList<>();

    // Переименовать Order -> Enrollment в домене (рекомендация).
    @OneToMany(mappedBy = "workshop", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Enrollment> enrollments = new ArrayList<>();

    // Связи для фильтров
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "language_id")
    private Language language;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "age_group_id")
    private AgeGroup ageGroup;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
        if (status == null) status = WorkshopStatus.DRAFT;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

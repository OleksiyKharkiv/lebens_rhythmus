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
    private Long id;

    @NotBlank
    @Size(max = 200)
    @Column(name = "workshop_name", nullable = false)
    private String workshopName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    /**
     * Overall participant limit (if applicable).
     * However, when groups exist, capacity is better stored at the Group level.
     */
    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column(name = "price")
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private WorkshopStatus status;

    // venue moved to Group (LR-015, 2026-08-05) — a workshop's groups can
    // run at different times, each needing its own place; venue is a
    // property of the scheduled session, not the workshop as a whole.
    // See V4__venue_to_group_level_plus_room.sql.

    // Main teacher (for multiple teachers use ManyToMany or separate
    // TeacherWorkshop entity). LR-072 — was User-typed (inconsistent
    // with the correctly Teacher-typed Group.teacher), migrated to
    // Teacher to match; see V9__migrate_workshop_teacher_to_teacher.sql
    // for the data-remapping this required.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    // LR-070 (LR-ADR-021) — zero-to-many, unidirectional: a Course can
    // include several Workshops, each Workshop belongs to at most one
    // Course. Nullable — "Workshop without a Course" is a valid case.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    // In the ERM there is a separate GroupWorkshop entity — it needs to be implemented.
    // Here simplified: list of groups (each group contains date/time/capacity)
    @OneToMany(mappedBy = "workshop", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Group> groups = new ArrayList<>();
    // Performances / presentations
    @OneToMany(mappedBy = "workshop", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Performance> performances = new ArrayList<>();

    // Files — may need to rename an entity to WorkshopFile/Attachment,
    // there's a risk of conflict with java.io.File
    @OneToMany(mappedBy = "workshop", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WorkshopFile> files = new ArrayList<>();

    // Remember to rename Order -> Enrollment in the domain.
    @OneToMany(mappedBy = "workshop", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Enrollment> enrollments = new ArrayList<>();

    // Relationships for filters
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
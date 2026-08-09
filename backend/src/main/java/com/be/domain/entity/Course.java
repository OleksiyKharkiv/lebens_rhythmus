package com.be.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Purely descriptive/marketing entity — no schedule fields (LR-ADR-023,
 * Roundtable #8). Regularity of occurrence lives on Group (LR-081), not
 * here, mirroring the Workshop -> Group split (LR-015).
 */
@Entity
@Table(name = "courses")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "title_de", nullable = false)
    private String titleDe;

    @NotBlank
    @Column(name = "title_en", nullable = false)
    private String titleEn;

    @NotBlank
    @Column(name = "title_ua", nullable = false)
    private String titleUa;

    @Column(name = "description_de", columnDefinition = "TEXT")
    private String descriptionDe;

    @Column(name = "description_en", columnDefinition = "TEXT")
    private String descriptionEn;

    @Column(name = "description_ua", columnDefinition = "TEXT")
    private String descriptionUa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "age_group_id")
    private AgeGroup ageGroup;

    // Headline/marketing teacher for the course page — same temporary
    // User-typed shape as Workshop.teacher (not Teacher), see LR-072.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private User teacher;

    @Builder.Default
    @Column(name = "is_online", nullable = false)
    private boolean isOnline = false;

    @Builder.Default
    @Column(name = "is_synchronous", nullable = false)
    private boolean isSynchronous = true;

    @Builder.Default
    @Column(name = "has_recordings", nullable = false)
    private boolean hasRecordings = false;

    // Single source of ZFU-compliance-sensitive format text — the public
    // course page renders this field, not separately hand-written text
    // per page (docs/compliance/tlab29-zfu-compliance-brief.md).
    @Column(name = "format_disclaimer_de", columnDefinition = "TEXT")
    private String formatDisclaimerDe;

    @Column(name = "format_disclaimer_en", columnDefinition = "TEXT")
    private String formatDisclaimerEn;

    @Column(name = "format_disclaimer_ua", columnDefinition = "TEXT")
    private String formatDisclaimerUa;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

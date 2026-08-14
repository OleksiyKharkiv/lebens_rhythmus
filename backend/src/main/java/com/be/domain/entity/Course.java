package com.be.domain.entity;

import com.be.domain.entity.enums.CourseStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
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

    // Urgent ticket 2026-08-14 (Olena): price + a short free-text note on
    // what the price covers (e.g. "12 sessions over 3 months, monthly
    // payment possible, excl. VAT §19..."), a background image for the
    // description block, and a DRAFT/PUBLISHED/... status mirroring
    // Workshop's (see WorkshopStatus) — kept as its own CourseStatus enum
    // rather than reusing WorkshopStatus, Course is a separate concept.
    @Column(name = "price")
    private BigDecimal price;

    @Column(name = "price_description", length = 1000)
    private String priceDescription;

    // URL only, not a real upload — see docs/context/CHANGELOG.md 2026-08-14
    // entry for why (no file-upload infra exists anywhere in this project
    // yet; a real upload pipeline is a separate, larger decision).
    @Column(name = "background_image_url", length = 2048)
    private String backgroundImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50, nullable = false)
    private CourseStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
        if (status == null) status = CourseStatus.DRAFT;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

package com.be.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseCreateDTO {
    @NotBlank
    private String titleDe;
    @NotBlank
    private String titleEn;
    @NotBlank
    private String titleUa;

    private String descriptionDe;
    private String descriptionEn;
    private String descriptionUa;

    private Long ageGroupId;
    private Long teacherId;

    private Boolean isOnline;
    private Boolean isSynchronous;
    private Boolean hasRecordings;

    private String formatDisclaimerDe;
    private String formatDisclaimerEn;
    private String formatDisclaimerUa;

    // Urgent ticket 2026-08-14 — price/priceDescription/backgroundImageUrl
    // are genuinely optional and clearable via the admin form; handled
    // authoritative-on-update in CourseService (not skip-if-null — that
    // exact bug class is documented in docs/context/KNOWN_ISSUES.md).
    private BigDecimal price;
    @Size(max = 1000)
    private String priceDescription;
    private String backgroundImageUrl;
    // "DRAFT"/"PUBLISHED"/"ARCHIVED"/"CANCELLED" — see CourseStatus. Never
    // cleared to null via the admin form (always a real selected value),
    // so skip-if-null in CourseService is correct here, unlike the three
    // fields above.
    private String status;
}

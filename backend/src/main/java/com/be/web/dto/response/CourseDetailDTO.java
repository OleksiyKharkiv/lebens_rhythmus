package com.be.web.dto.response;

import com.be.domain.entity.RecurrenceDay;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseDetailDTO {
    private Long id;
    private String titleDe;
    private String titleEn;
    private String titleUa;
    private String descriptionDe;
    private String descriptionEn;
    private String descriptionUa;
    private Long ageGroupId;
    private String ageGroupName;
    private UserBasicDTO teacher;
    private Boolean isOnline;
    private Boolean isSynchronous;
    private Boolean hasRecordings;
    private String formatDisclaimerDe;
    private String formatDisclaimerEn;
    private String formatDisclaimerUa;
    private BigDecimal price;
    private String priceDescription;
    private String backgroundImageUrl;
    private String status;

    // 2026-08-14 — surfaced so the public course page can compute total
    // session count / duration itself (start + duration + weekdays), same
    // ingredients SessionService.generateSessionsFromRecurrence uses
    // server-side. Sourced from this Course's linked Group (LR-081,
    // "one Course = one Group" MVP scope) — not from the Group entity
    // itself, since GET /groups requires auth and this page is public.
    private LocalDate scheduleStartDate;
    private LocalDate scheduleEndDate;
    private List<RecurrenceDay> scheduleDays;
}

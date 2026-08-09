package com.be.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}

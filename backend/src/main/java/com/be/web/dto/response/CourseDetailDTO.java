package com.be.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}

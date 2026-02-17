package com.be.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupDTO {
    private Long id;

    // multilingual titles (exposed for admin UI)
    private String titleDe;
    private String titleEn;
    private String titleUa;

    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;

    private Integer capacity;
    private Integer enrolledCount;

    // relations (IDs only)
    private Long workshopId;
    private String workshopTitle;
    private Long activityId;
    private Long teacherId;
    private Long ageGroupId;
    private Long languageId;

    private boolean active;
}
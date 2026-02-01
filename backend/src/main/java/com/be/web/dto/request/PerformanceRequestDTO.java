package com.be.web.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceRequestDTO {
    private Long workshopId;
    private String title;
    private String description;
    private LocalDateTime performanceDate;
    private String venue;
    private Integer maxAttendees;
    private String status;
}

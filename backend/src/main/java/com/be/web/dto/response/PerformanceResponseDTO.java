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
public class PerformanceResponseDTO {
    private Long id;
    private Long workshopId;
    private String workshopTitle;
    private String title;
    private String description;
    private LocalDateTime performanceDate;
    private String venue;
    private Integer maxAttendees;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
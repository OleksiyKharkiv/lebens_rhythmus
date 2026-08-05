package com.be.web.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkshopCreateDTO {
    private String title;
    private String description;
    private Long teacherId;
    private LocalDate startDate;
    private LocalDate endDate;
    // venueId removed (LR-015) — venue now lives on Group, not Workshop.
    private Integer maxParticipants;
    private BigDecimal price;
    private String status; // DRAFT / PUBLISHED
}
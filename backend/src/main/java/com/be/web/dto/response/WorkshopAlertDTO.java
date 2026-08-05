package com.be.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// M5 — Круглый стол #3 (LR-015), 2026-08-05. level: info | warning | urgent | critical.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkshopAlertDTO {
    private Long groupId;
    private String workshopTitle;
    private String groupTitle;
    private LocalDateTime startDateTime;
    private long daysUntilStart;
    private double fillRatio;
    private String level;
}

package com.be.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// M1 — Круглый стол #3 (LR-015), 2026-08-05.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupFillRateDTO {
    private Long groupId;
    private String workshopTitle;
    private String groupTitle;
    private LocalDateTime startDateTime;
    private int capacity;
    private int enrolledCount;
    private double fillRatio;
}

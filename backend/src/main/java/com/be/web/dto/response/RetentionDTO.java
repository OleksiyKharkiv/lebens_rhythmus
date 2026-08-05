package com.be.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// M6 — Круглый стол #3 (LR-015), 2026-08-05. "Repeat" = >=2 distinct
// workshops with a CONFIRMED enrollment, ever (lifetime, not windowed).
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetentionDTO {
    private long totalCustomers;
    private long repeatCustomers;
    private double retentionRate;
}

package com.be.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// LR-015 admin/owner dashboard metrics (Круглый стол #2/#3, 2026-08-05).
// M2/M3 (revenue, near-term fill) deliberately absent — blocked on the
// registration/payment confirmation mechanism, see LR-017.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminMetricsDTO {
    private List<GroupFillRateDTO> fillRates;
    private List<RegistrationTrendPointDTO> registrationTrend;
    private List<WorkshopAlertDTO> attentionAlerts;
    private RetentionDTO retention;
}

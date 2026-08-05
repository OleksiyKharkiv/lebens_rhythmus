package com.be.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

// M4 — Круглый стол #3 (LR-015), 2026-08-05.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationTrendPointDTO {
    private LocalDate date;
    private long newUsers;
}

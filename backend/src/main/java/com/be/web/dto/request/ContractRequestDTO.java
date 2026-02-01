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
public class ContractRequestDTO {
    private String contractNumber;
    private String title;
    private String partyName;
    private String contact;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String contractUrl;
}

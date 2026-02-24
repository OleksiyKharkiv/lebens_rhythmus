package com.be.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractResponseDTO {
    private Long id;
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
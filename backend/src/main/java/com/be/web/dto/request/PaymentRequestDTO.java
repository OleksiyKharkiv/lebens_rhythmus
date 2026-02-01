package com.be.web.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDTO {
    private Long orderId;
    private Long userId;
    private BigDecimal amount;
    private String currency;
    private String provider;
    private String methodName;
    private String transactionId;
    private String status;
    private LocalDateTime paidAt;
    private String note;
}

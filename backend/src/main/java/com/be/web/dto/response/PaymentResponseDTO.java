package com.be.web.dto.response;

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
public class PaymentResponseDTO {
    private Long id;
    private Long orderId;
    private String orderNumber;
    private Long userId;
    private String username;
    private BigDecimal amount;
    private String currency;
    private String provider;
    private String methodName;
    private String transactionId;
    private String status;
    private LocalDateTime paidAt;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
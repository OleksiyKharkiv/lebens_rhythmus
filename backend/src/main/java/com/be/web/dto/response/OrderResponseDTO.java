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
public class OrderResponseDTO {
    private Long id;
    private String orderNumber;
    private Long userId;
    private String username;
    private Long participantId;
    private String participantName;
    private Long workshopId;
    private String workshopTitle;
    private Long eventId;
    private String eventTitle;
    private BigDecimal amount;
    private String currency;
    private Integer quantity;
    private String status;
    private String note;
    private Long contractId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
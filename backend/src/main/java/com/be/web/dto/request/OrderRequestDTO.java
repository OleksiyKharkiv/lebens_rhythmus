package com.be.web.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequestDTO {
    private String orderNumber;
    private Long participantId;
    private Long workshopId;
    private Long eventId;
    private BigDecimal amount;
    private String currency;
    private Integer quantity;
    private String status;
    private String note;
    private Long contractId;
}

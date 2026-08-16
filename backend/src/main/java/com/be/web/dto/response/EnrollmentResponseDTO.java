package com.be.web.dto.response;

import com.be.domain.entity.enums.EnrollmentStatus;
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
public class EnrollmentResponseDTO {
    private Long id;
    private Long workshopId;
    private String workshopTitle;
    private Long courseId;
    private String courseTitle;
    private Long groupId;
    private String groupTitle;
    private EnrollmentStatus status;
    // LR-084 — set only for paid enrollments (see EnrollmentService); lets
    // the dashboard show "awaiting payment: 165 EUR" instead of a bare
    // PENDING with no context.
    private Long orderId;
    private BigDecimal orderAmount;
    private String orderCurrency;
    private LocalDateTime createdAt;
}
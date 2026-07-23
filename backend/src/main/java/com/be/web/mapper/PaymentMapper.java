package com.be.web.mapper;

import com.be.domain.entity.Payment;
import com.be.web.dto.request.PaymentRequestDTO;
import com.be.web.dto.response.PaymentResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentResponseDTO toResponseDTO(Payment payment) {
        if (payment == null) return null;
        return PaymentResponseDTO.builder()
                .id(payment.getId())
                .orderId(payment.getOrder() != null ? payment.getOrder().getId() : null)
                .orderNumber(payment.getOrder() != null ? payment.getOrder().getOrderNumber() : null)
                .userId(payment.getUser() != null ? payment.getUser().getId() : null)
                .username(payment.getUser() != null ? (payment.getUser().getFirstName() + " " + payment.getUser().getLastName()) : null)
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .provider(payment.getProvider())
                .methodName(payment.getMethodName())
                .transactionId(payment.getTransactionId())
                .status(payment.getStatus())
                .paidAt(payment.getPaidAt())
                .note(payment.getNote())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }

    /**
     * Self-view mapping for GET /api/v1/payments/me (LR-ADR-016, LR-004) —
     * omits `note`, since it's documented as an admin/accounting reference
     * (may hold internal context not meant for the customer) and this is
     * the only endpoint a non-admin can reach directly.
     */
    public PaymentResponseDTO toSelfViewDTO(Payment payment) {
        if (payment == null) return null;
        PaymentResponseDTO dto = toResponseDTO(payment);
        dto.setNote(null);
        return dto;
    }

    public Payment fromRequestDTO(PaymentRequestDTO dto) {
        if (dto == null) return null;
        return Payment.builder()
                .amount(dto.getAmount())
                .currency(dto.getCurrency())
                .provider(dto.getProvider())
                .methodName(dto.getMethodName())
                .transactionId(dto.getTransactionId())
                .status(dto.getStatus())
                .paidAt(dto.getPaidAt())
                .note(dto.getNote())
                .build();
    }
}
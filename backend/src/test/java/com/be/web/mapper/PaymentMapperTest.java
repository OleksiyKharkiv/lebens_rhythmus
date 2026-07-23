package com.be.web.mapper;

import com.be.domain.entity.Payment;
import com.be.web.dto.response.PaymentResponseDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * toSelfViewDTO strips `note` before it ever reaches the customer-facing
 * /payments/me endpoint (LR-004) — this is the only place that behavior is
 * actually exercised, since PaymentMapper is mocked in PaymentControllerTest.
 */
class PaymentMapperTest {

    private final PaymentMapper mapper = new PaymentMapper();

    @Test
    void toSelfViewDTO_omitsNoteButKeepsOtherFields() {
        Payment payment = Payment.builder()
                .id(1L)
                .amount(new BigDecimal("49.00"))
                .currency("EUR")
                .status("COMPLETED")
                .note("Refunded per support ticket #123 — internal, do not show customer")
                .build();

        PaymentResponseDTO dto = mapper.toSelfViewDTO(payment);

        assertThat(dto.getNote()).isNull();
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getAmount()).isEqualByComparingTo("49.00");
        assertThat(dto.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void toResponseDTO_stillIncludesNoteForAdminView() {
        Payment payment = Payment.builder().id(1L).note("internal note").build();

        PaymentResponseDTO dto = mapper.toResponseDTO(payment);

        assertThat(dto.getNote()).isEqualTo("internal note");
    }
}

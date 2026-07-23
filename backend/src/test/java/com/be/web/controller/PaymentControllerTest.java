package com.be.web.controller;

import com.be.config.CorsProperties;
import com.be.config.SecurityConfig;
import com.be.domain.entity.Payment;
import com.be.service.PaymentService;
import com.be.web.dto.response.PaymentResponseDTO;
import com.be.web.mapper.PaymentMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression test for the self-scoped payment history endpoint added after
 * architect-reviewer flagged that PaymentController had no route a regular
 * user could reach at all — every method required ADMIN/BUSINESS_OWNER,
 * blocking LR-ADR-016's personal dashboard requirement.
 * <p>
 * Asserts exactly what the reviewer asked to be covered: a user only ever
 * gets payments resolved via their own JWT-derived id (no client-supplied
 * userId parameter exists on this endpoint to tamper with), and an
 * unauthenticated request is rejected outright.
 */
@WebMvcTest(PaymentController.class)
@Import(SecurityConfig.class)
@EnableConfigurationProperties(CorsProperties.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private PaymentMapper paymentMapper;

    @Test
    void getMyPayments_returnsOnlyTheAuthenticatedUsersOwnPayments() throws Exception {
        Payment payment = Payment.builder().id(1L).amount(new BigDecimal("49.00")).status("COMPLETED").build();
        PaymentResponseDTO dto = PaymentResponseDTO.builder()
                .id(1L).amount(new BigDecimal("49.00")).status("COMPLETED").build();

        when(paymentService.getMyPayments(eq(42L))).thenReturn(List.of(payment));
        when(paymentMapper.toSelfViewDTO(payment)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/payments/me")
                        .with(jwt().jwt(j -> j.claim("id", 42))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }

    @Test
    void getMyPayments_rejectsUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get("/api/v1/payments/me"))
                .andExpect(status().isUnauthorized());
    }
}

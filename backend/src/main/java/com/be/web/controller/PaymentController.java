package com.be.web.controller;

import com.be.config.JwtAuthUtils;
import com.be.domain.entity.Payment;
import com.be.service.PaymentService;
import com.be.web.dto.request.PaymentRequestDTO;
import com.be.web.dto.response.PaymentResponseDTO;
import com.be.web.mapper.PaymentMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentMapper paymentMapper;

    public PaymentController(PaymentService paymentService, PaymentMapper paymentMapper) {
        this.paymentService = paymentService;
        this.paymentMapper = paymentMapper;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER')")
    public ResponseEntity<List<PaymentResponseDTO>> getAll() {
        List<Payment> payments = paymentService.getAll();
        return ResponseEntity.ok(payments.stream()
                .map(paymentMapper::toResponseDTO)
                .collect(Collectors.toList()));
    }

    /**
     * GET /api/v1/payments/me — self-scoped payment history (LR-ADR-016).
     * Matched before /{id} by Spring's own literal-over-variable path
     * precedence (same pattern already relied on by UserController's
     * /users/me vs /users/{userId}).
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PaymentResponseDTO>> getMyPayments(@AuthenticationPrincipal Jwt jwt) {
        Long userId = JwtAuthUtils.extractUserId(jwt);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        List<Payment> payments = paymentService.getMyPayments(userId);
        return ResponseEntity.ok(payments.stream()
                .map(paymentMapper::toSelfViewDTO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER')")
    public ResponseEntity<PaymentResponseDTO> getById(@PathVariable Long id) {
        Payment payment = paymentService.getById(id);
        return ResponseEntity.ok(paymentMapper.toResponseDTO(payment));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER')")
    public ResponseEntity<PaymentResponseDTO> create(@Valid @RequestBody PaymentRequestDTO dto) {
        Payment created = paymentService.create(dto);
        return ResponseEntity.status(201).body(paymentMapper.toResponseDTO(created));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        paymentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
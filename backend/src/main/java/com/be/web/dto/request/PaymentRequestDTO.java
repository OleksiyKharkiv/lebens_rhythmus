package com.be.web.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// LR-021 — had zero validation annotations on any field. Deliberately
// scoped narrower than LR-012's copy-paste-able @Size fix (see that
// ticket's own note in tickets.md): `status` is left as a plain
// length-capped string, NOT converted to an enum — Payment.status's own
// entity comment already flags "Kept as String for now — optionally
// switch to enum", and there is no real value domain established
// anywhere in the codebase to build an enum from (grepped for it) —
// inventing one now would be guessing at a product decision, not fixing
// a validation gap. Bounds below match the entity's actual @Column
// definitions where explicit, or Hibernate's implicit 255-char default
// where not.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDTO {
    private Long orderId;
    private Long userId;

    @NotNull(message = "Amount is mandatory")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @Pattern(regexp = "[A-Z]{3}", message = "Currency must be a 3-letter ISO 4217 code, e.g. EUR")
    private String currency;

    @Size(max = 255)
    private String provider;

    @Size(max = 255)
    private String methodName;

    @Size(max = 200)
    private String transactionId;

    @Size(max = 40)
    private String status;

    private LocalDateTime paidAt;

    @Size(max = 1000)
    private String note;
}
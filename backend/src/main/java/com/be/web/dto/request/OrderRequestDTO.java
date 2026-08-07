package com.be.web.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// LR-021 — same reasoning as the sibling fix on PaymentRequestDTO: `status`
// deliberately left as a length-capped string, not an enum (no established
// value domain in the codebase to build one from — a real product
// decision, not guessed here). Bounds match Order's actual @Column
// definitions (orderNumber max 100, note max 2000). orderNumber is NOT
// @NotBlank here despite the entity's nullable=false — this DTO is shared
// between create and update (OrderController.update()), and
// OrderService.update() never reads orderNumber at all; requiring it on
// every PUT would be an artificial requirement update() doesn't need. The
// DB's own NOT NULL constraint still catches a genuinely missing value on
// create.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequestDTO {
    @Size(max = 100)
    private String orderNumber;

    private Long participantId;
    private Long workshopId;
    private Long eventId;

    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @Pattern(regexp = "[A-Z]{3}", message = "Currency must be a 3-letter ISO 4217 code, e.g. EUR")
    private String currency;

    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @Size(max = 32)
    private String status;

    @Size(max = 2000)
    private String note;

    private Long contractId;
}
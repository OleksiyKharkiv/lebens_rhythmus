package com.be.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_tx", columnList = "transaction_id"),
        @Index(name = "idx_payment_order", columnList = "order_id")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Relation to order (the order this payment belongs to).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    /**
     * Payer (optional) — the user who executed the payment.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * Amount (gross).
     */
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    /**
     * Currency code, e.g. "EUR".
     */
    @Column(length = 8)
    private String currency;

    /**
     * Payment provider / gateway (Stripe, PayPal, Sepa, etc.).
     */
    private String provider;

    /**
     * Free-text method name (card, sepa, invoice...). Useful until the PaymentMethod entity is implemented.
     */
    private String methodName;

    /**
     * External transaction id returned by the provider.
     */
    @Column(name = "transaction_id", length = 200)
    private String transactionId;

    /**
     * Payment status (e.g., PENDING / COMPLETED / FAILED / REFUNDED).
     * Kept as String for now — optionally switch to enum.
     */
    @Column(length = 40)
    private String status;

    /**
     * When the provider confirmed the payment.
     */
    private LocalDateTime paidAt;

    /**
     * Optional refund reference or note
     */
    @Column(length = 1000)
    private String note;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
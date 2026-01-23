package com.be.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Order entity — represents a purchase / booking made by a User (or for a Participant).
 * Links to Workshop/Event and holds Payments.
 */
@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_order_number", columnList = "order_number"),
        @Index(name = "idx_order_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * External order number / invoice reference (human-friendly).
     */
    @Column(name = "order_number", nullable = false, unique = true)
    @Size(max = 100)
    private String orderNumber;

    /**
     * The user who placed the order (may be the account owner).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * Optional participant for whom the order was placed (User can have multiple Participant profiles).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id")
    private Participant participant;

    /**
     * If the order is for a workshop, link here.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workshop_id")
    private Workshop workshop;

    /**
     * If the order is for an event (e.g. performance) — alternative target.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    /**
     * Total amount for the order (gross). Use BigDecimal for money.
     */
    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    /**
     * Currency code, e.g. "EUR".
     */
    @Column(length = 8)
    private String currency;

    /**
     * Quantity — e.g. number of seats/tickets in this order.
     */
    private Integer quantity;

    /**
     * Order status (DRAFT / PENDING / PAID / CANCELLED / REFUNDED) — stored as string for flexibility.
     * You may replace with enum OrderStatus later.
     */
    @Column(length = 32)
    private String status;

    /**
     * Optional internal note / invoice note.
     */
    @Column(length = 2000)
    private String note;

    /**
     * Payments related to this order (1..N). Payment entity should have field 'order' mapped back.
     * Cascade so payments persist with order; orphanRemoval true to allow removing payments via order.
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Payment> payments = new ArrayList<>();

    /**
     * Optional contract reference (if a Contract entity is used).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private Contract contract;

    /**
     * Timestamps
     */
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
        // generate minimal orderNumber if not provided (can be overridden by service)
        if (orderNumber == null || orderNumber.isBlank()) {
            orderNumber = "ORD-" + System.currentTimeMillis();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Convenience helper to add payment
     */
    public void addPayment(Payment p) {
        if (p == null) return;
        payments.add(p);
        p.setOrder(this);
    }

    /**
     * Convenience helper to remove payment
     */
    public void removePayment(Payment p) {
        if (p == null) return;
        payments.remove(p);
        p.setOrder(null);
    }
}

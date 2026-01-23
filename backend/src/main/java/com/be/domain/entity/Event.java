package com.be.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Event — public performance / single event instance.
 * Can be linked to a Workshop (e.g. final performance) or be standalone (Performance).
 */
@Entity
@Table(name = "events", indexes = {
        @Index(name = "idx_event_start", columnList = "start_date_time"),
        @Index(name = "idx_event_venue", columnList = "venue_id")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Human title
     */
    private String title;

    @Column(length = 2000)
    private String description;

    /**
     * Date/time of event start & end
     */
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;

    /**
     * Venue
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id")
    private Venue venue;

    /**
     * Optionally tied to a workshop (e.g. workshop final performance)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workshop_id")
    private Workshop workshop;

    /**
     * Contract associated with the event (if any)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private Contract contract;

    /**
     * Price for ticket / entry (if applicable)
     */
    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    @Column(length = 8)
    private String currency;

    /**
     * Maximum audience / seats
     */
    private Integer capacity;

    /**
     * Event status (SCHEDULED / CANCELLED / COMPLETED / POSTPONED)
     */
    @Column(length = 40)
    private String status;

    /**
     * Orders for this event (if ticketed). Order.order.event maps back here.
     */
    @OneToMany(mappedBy = "event", cascade = CascadeType.PERSIST)
    @Builder.Default
    private List<Order> orders = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // convenience
    public void addOrder(Order o) {
        if (o == null) return;
        orders.add(o);
        o.setEvent(this);
    }

    public void removeOrder(Order o) {
        if (o == null) return;
        orders.remove(o);
        o.setEvent(null);
    }
}
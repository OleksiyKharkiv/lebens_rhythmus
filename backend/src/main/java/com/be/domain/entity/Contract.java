package com.be.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Contract — represents an agreement / contract (e.g. with a venue, a partner, or a client)
 * which can be associated with Events.
 */
@Entity
@Table(name = "contracts", indexes = {
        @Index(name = "idx_contract_number", columnList = "contract_number")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Human-friendly contract number / reference
     */
    @Column(name = "contract_number", nullable = false, unique = true, length = 120)
    private String contractNumber;

    /**
     * Title / subject of the contract
     */
    private String title;

    /**
     * Party (counterparty) name (e.g. venue owner or client)
     */
    private String partyName;

    /**
     * Optional contact person / email
     */
    private String contact;

    /**
     * Contract period (optional)
     */
    private LocalDate startDate;
    private LocalDate endDate;

    /**
     * Contracted total amount
     */
    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    /**
     * Currency, e.g. EUR
     */
    @Column(length = 8)
    private String currency;

    /**
     * Status (DRAFT / ACTIVE / CLOSED / CANCELLED / ARCHIVED)
     */
    @Column(length = 40)
    private String status;

    /**
     * Optional URL to stored contract file (or relation to ContractFile entity).
     */
    private String contractUrl;

    /**
     * Events linked to this contract (one contract may cover multiple events)
     */
    @OneToMany(mappedBy = "contract", cascade = CascadeType.PERSIST)
    @Builder.Default
    private List<Event> events = new ArrayList<>();

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
    public void addEvent(Event e) {
        if (e == null) return;
        events.add(e);
        e.setContract(this);
    }

    public void removeEvent(Event e) {
        if (e == null) return;
        events.remove(e);
        e.setContract(null);
    }
}
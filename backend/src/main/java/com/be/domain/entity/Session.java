package com.be.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * One scheduled day of a multi-day Group (LR-ADR-022). Group keeps the
 * single enrollment/capacity roster it already had — Session only carries
 * per-day start/end/venue, it is not a separate registration target.
 */
@Entity
@Table(name = "group_sessions",
        indexes = {
                @Index(name = "idx_group_sessions_group", columnList = "group_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @Column(name = "start_date_time", nullable = false)
    private LocalDateTime startDateTime;

    @Column(name = "end_date_time")
    private LocalDateTime endDateTime;

    // Deliberately its own FK, not shared with Group.venue — different
    // days of the same multi-day workshop can run at different places
    // (LR-067, customer's explicit п.4 request).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id")
    private Venue venue;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

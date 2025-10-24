package com.be.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Set;

@Entity
@Table(name = "activities")
@NoArgsConstructor
@AllArgsConstructor
public class Activity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titleDe;

    @Column(nullable = false)
    private String titleEn;

    @Column(nullable = false)
    private String titleUa;

    @Column(nullable = false)
    private String descriptionDe;

    @Column(nullable = false)
    private String descriptionEn;

    @Column(nullable = false)
    private String descriptionUa;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private int durationMinutes;

    @Column(nullable = false)
    private boolean active;

    @OneToMany(mappedBy = "activity")
    private Set<Group> groups;
}

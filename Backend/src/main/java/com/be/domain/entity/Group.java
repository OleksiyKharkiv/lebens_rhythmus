package com.be.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import static jakarta.persistence.FetchType.LAZY;

@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "groups")
public class Group {
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
    private int maxParticipants;

    @ManyToOne(fetch = LAZY)
    private Activity activity;

    @ManyToOne(fetch = LAZY) private AgeGroup ageGroup;

    @ManyToOne(fetch = LAZY)
    private Language language;


}
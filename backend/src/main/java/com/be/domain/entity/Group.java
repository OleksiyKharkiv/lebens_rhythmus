package com.be.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

import static jakarta.persistence.FetchType.LAZY;


@Entity
@Table(name = "groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    @ManyToOne(fetch = LAZY)
    private AgeGroup ageGroup;

    @ManyToOne(fetch = LAZY)
    private Language language;

    @ManyToOne(fetch = LAZY)
    private Teacher teacher;

    @OneToMany(mappedBy = "group")
    private Set<Participant> participants;

    @Column(nullable = false)
    private boolean active = true;

}
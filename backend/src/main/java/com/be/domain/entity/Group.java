package com.be.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "groups")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "age_group_id", nullable = false)
    private AgeGroup ageGroup;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "language_id", nullable = false)
    private Language language;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    @Builder.Default
    @OneToMany(mappedBy = "group")
    private Set<Participant> participants = new HashSet<>();

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}

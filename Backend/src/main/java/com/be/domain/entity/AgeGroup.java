package com.be.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Table(name = "age_groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgeGroup {
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
    private Integer minAge;

    @Column(nullable = false)
    private Integer maxAge;

    @OneToMany(mappedBy = "ageGroup")
    private Set<Group> groups;
}

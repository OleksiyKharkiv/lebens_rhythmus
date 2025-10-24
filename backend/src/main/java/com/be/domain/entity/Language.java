package com.be.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "languages")
public class Language {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nameDe;

    @Column(nullable = false)
    private String nameEn;

    @Column(nullable = false)
    private String nameUa;

    @Column(nullable = false, length = 2)
    private String code;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "language")
    private Set<Group> groups;
}

package com.be.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "teachers")
@Builder
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Teacher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String title;

    @Builder.Default
    @Column(nullable = false)
    private boolean approved = false;

    @Column(columnDefinition = "TEXT")
    private String bioDe;

    @Column(columnDefinition = "TEXT")
    private String bioEn;

    @Column(columnDefinition = "TEXT")
    private String bioUa;

    @Builder.Default
    @OneToMany(mappedBy = "teacher")
    private Set<Group> groups = new HashSet<>();

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}
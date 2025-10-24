package com.be.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Table(name = "teachers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    @Column(columnDefinition = "TEXT")
    private String bioDe;

    @Column(columnDefinition = "TEXT")
    private String bioEn;

    @Column(columnDefinition = "TEXT")
    private String bioUa;

    @OneToMany(mappedBy = "teacher")
    private Set<Group> groups;

    @Column(nullable = false)
    private boolean active = true;
}

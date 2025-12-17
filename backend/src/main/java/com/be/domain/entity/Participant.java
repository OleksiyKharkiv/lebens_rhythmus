package com.be.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "participants")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Participant {
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
    private LocalDate birthDate;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}

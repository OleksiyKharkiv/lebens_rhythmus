package com.be.domain.entity;

import com.be.domain.entity.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private int failedLoginAttempts;
    private LocalDateTime lockUntil;
    private boolean enable = true;

    private boolean acceptedTerms = false;

    private String phone;


    @Enumerated(EnumType.STRING)
    private Role role;

    private String firstName;
    private String lastName;


}

package com.be.domain.entity;

import com.be.domain.entity.enums.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_email", columnList = "email"),
        @Index(name = "idx_user_role", columnList = "role"),
        @Index(name = "idx_user_lock_until", columnList = "lock_until")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //    Registration data
    @Column(nullable = false, unique = true)
    private String email;
    @Column(nullable = false)
    private String password;

    //    Profil
    @Size(min = 2, max = 50)
    private String firstName;
    @Size(min = 2, max = 50)
    private String lastName;
    private String phone;
    private LocalDate birthDate;

    //    Security
    private int failedLoginAttempts;
    private LocalDateTime lockUntil;
    private Boolean enabled = true;
    private Boolean emailVerified = false;

    // GDPR accept
    private Boolean acceptedTerms = false;
    private Boolean privacyPolicyAccepted = false;
    private LocalDateTime termsAcceptedAt;
    private LocalDateTime privacyPolicyAcceptedAt;

    //Role
    @Enumerated(EnumType.STRING)
    private Role role;

    //Account address
    private String address;
    private String city;
    private String zipCode;
    private String country = "Deutschland";

    //    for teachers
    private String title;
    private String bio;
    private String iban;
    private String taxId;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
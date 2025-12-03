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

    // Registration data
    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    // Profile
    @Size(min = 2, max = 50)
    private String firstName;

    @Size(min = 2, max = 50)
    private String lastName;

    private String phone;
    private LocalDate birthDate;

    // Security
    private int failedLoginAttempts;
    private LocalDateTime lockUntil;

    // NOTE: some legacy DBs use column name "enable" — маппим явно и ставим NOT NULL
    @Builder.Default
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Builder.Default
    private Boolean emailVerified = false;

    // GDPR accept
    @Builder.Default
    private Boolean acceptedTerms = false;

    @Builder.Default
    private Boolean privacyPolicyAccepted = false;

    private LocalDateTime termsAcceptedAt;
    private LocalDateTime privacyPolicyAcceptedAt;

    // Role
    @Enumerated(EnumType.STRING)
    private Role role;

    // Account address
    private String address;
    private String city;
    private String zipCode;

    @Builder.Default
    private String country = "Deutschland";

    // for teachers
    private String title;
    private String bio;
    private String iban;
    private String taxId;

    // timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        // ensure not-null booleans in case builder or mapper omitted them
        if (enabled == null) enabled = true;
        if (emailVerified == null) emailVerified = false;
        if (acceptedTerms == null) acceptedTerms = false;
        if (privacyPolicyAccepted == null) privacyPolicyAccepted = false;
        if (country == null) country = "Deutschland";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

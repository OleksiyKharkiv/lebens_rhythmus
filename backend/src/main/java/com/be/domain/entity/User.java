package com.be.domain.entity;

import com.be.domain.entity.enums.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_email", columnList = "email"),
        @Index(name = "idx_user_role", columnList = "role"),
        @Index(name = "idx_user_lock_until", columnList = "lock_until")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;

    @Builder.Default
    private boolean emailVerified = false;

    // GDPR
    @Builder.Default
    private boolean acceptedTerms = false;

    @Builder.Default
    private boolean privacyPolicyAccepted = false;

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

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Order> orders = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Feedback> feedbacks = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserNotification> userNotifications = new ArrayList<>();

    // timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
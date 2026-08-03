package com.be.domain.entity;

import com.be.config.crypto.EncryptedStringConverter;
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

    // Profile — DSGVO-sensitive PII, encrypted at rest (see
    // EncryptedStringConverter). email stays plaintext (UNIQUE constraint,
    // used for login lookups — GCM's random IV would break both).
    // birthDate stays plaintext too: native DATE column, current converter
    // is String-only, a LocalDate-specific one is separate follow-up work.
    @Convert(converter = EncryptedStringConverter.class)
    @Size(min = 2, max = 50)
    private String firstName;

    @Convert(converter = EncryptedStringConverter.class)
    @Size(min = 2, max = 50)
    private String lastName;

    @Convert(converter = EncryptedStringConverter.class)
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

    // Verification token — only the SHA-256 hash is stored, never the
    // plaintext token (same reasoning as password hashing: a DB/backup leak
    // must not hand out working verification links). Cleared once verified.
    private String verificationTokenHash;
    private LocalDateTime verificationTokenExpiresAt;

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

    // Account address — encrypted at rest (see note above); country stays
    // plaintext (single low-cardinality value, not identifying by itself).
    @Convert(converter = EncryptedStringConverter.class)
    private String address;

    @Convert(converter = EncryptedStringConverter.class)
    private String city;

    @Convert(converter = EncryptedStringConverter.class)
    private String zipCode;

    @Builder.Default
    private String country = "Deutschland";

    // for teachers
    private String title;
    private String bio;

    // GoBD/DSGVO-sensitive — encrypted at rest, see EncryptedStringConverter.
    // Never query/filter on these columns (GCM ciphertext is non-deterministic).
    @Convert(converter = EncryptedStringConverter.class)
    private String iban;

    @Convert(converter = EncryptedStringConverter.class)
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
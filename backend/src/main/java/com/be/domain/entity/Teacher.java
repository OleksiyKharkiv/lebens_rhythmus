package com.be.domain.entity;

import com.be.config.crypto.EncryptedStringConverter;
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

    // DSGVO-sensitive PII — encrypted at rest, see EncryptedStringConverter.
    // Decryption is transparent at the JPA layer, so GET /teachers still
    // returns the real name — this only protects the raw DB/backup contents.
    // email is deliberately NOT encrypted: it has a UNIQUE constraint, and
    // AES-GCM's random IV makes the same plaintext produce different
    // ciphertext every time, which would make the constraint stop catching
    // real duplicates (see EncryptedStringConverter's own class javadoc).
    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false)
    private String firstName;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Convert(converter = EncryptedStringConverter.class)
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
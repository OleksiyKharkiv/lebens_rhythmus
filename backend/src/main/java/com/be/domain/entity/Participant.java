package com.be.domain.entity;

import com.be.config.crypto.EncryptedStringConverter;
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

    // DSGVO-sensitive PII — encrypted at rest, see EncryptedStringConverter.
    // email is deliberately NOT encrypted (UNIQUE constraint — AES-GCM's
    // random IV would break real duplicate detection). birthDate is
    // deliberately NOT encrypted either — it's a native DATE column and the
    // current converter is String-only; a LocalDate-specific converter is
    // tracked as separate follow-up work, not solved here.
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
    private LocalDate birthDate;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}
package com.be;

import com.be.config.crypto.EncryptedStringConverter;
import com.be.domain.entity.User;
import com.be.domain.entity.enums.Role;
import com.be.domain.repository.UserRepository;
import com.be.tools.PiiReencryptionRunner;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proof for LR-011's re-encryption tool: seeds a mix of legacy plaintext
 * (inserted via raw JDBC, bypassing the @Convert-annotated entity path —
 * exactly what a real pre-migration row looks like) and already-encrypted
 * rows (inserted the normal way, through the repository), then runs the
 * tool's own logic directly (not via CommandLineRunner.run(), to avoid
 * needing a second Spring context per test) and verifies:
 * dry run touches nothing, --apply migrates only the genuinely plaintext
 * row, and a normal entity read afterward succeeds without throwing (the
 * exact failure mode LR-011 is about).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class PiiReencryptionRunnerTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private EncryptedStringConverter converter;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    void dryRunLeavesDataUntouched_applyMigratesOnlyPlaintextRows() {
        // Legacy row: real plaintext sitting directly in the DB, as if this
        // user existed before EncryptedStringConverter was applied.
        User legacyUser = User.builder()
                .email("legacy-" + System.nanoTime() + "@example.com")
                .password("irrelevant")
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .build();
        User savedLegacy = userRepository.save(legacyUser);
        entityManager.flush();
        jdbcTemplate.update("UPDATE users SET first_name = ? WHERE id = ?", "Legacy Plaintext Name", savedLegacy.getId());

        // Already-migrated row: created the normal way, through the
        // converter, so the DB genuinely holds ciphertext.
        User modernUser = User.builder()
                .email("modern-" + System.nanoTime() + "@example.com")
                .password("irrelevant")
                .role(Role.USER)
                .firstName("Already Encrypted Name")
                .createdAt(LocalDateTime.now())
                .build();
        User savedModern = userRepository.save(modernUser);
        entityManager.flush();
        entityManager.clear();

        PiiReencryptionRunner runner = new PiiReencryptionRunner(jdbcTemplate, converter);

        // Dry run: nothing written.
        runner.run(); // no --apply
        assertThat(rawFirstName(savedLegacy.getId())).isEqualTo("Legacy Plaintext Name");

        // Confirm the legacy row would genuinely crash a normal entity read
        // right now — this IS the LR-011 problem, proving the test fixture
        // is realistic, not a strawman.
        entityManager.clear();
        assertThatDecryptingLegacyRowThrows(savedLegacy.getId());

        // Apply: legacy row gets encrypted, modern row is untouched.
        String modernRawBefore = rawFirstName(savedModern.getId());
        runner.run("--apply");

        assertThat(rawFirstName(savedModern.getId()))
                .as("already-encrypted row must not be re-written")
                .isEqualTo(modernRawBefore);

        String legacyRawAfter = rawFirstName(savedLegacy.getId());
        assertThat(legacyRawAfter).isNotEqualTo("Legacy Plaintext Name");
        assertThat(legacyRawAfter).doesNotContain("Legacy Plaintext Name");

        // The real proof: a normal entity read (through the converter) now
        // succeeds and returns the original plaintext, instead of throwing.
        entityManager.clear();
        User reloaded = userRepository.findById(savedLegacy.getId()).orElseThrow();
        assertThat(reloaded.getFirstName()).isEqualTo("Legacy Plaintext Name");
    }

    private String rawFirstName(Long id) {
        return jdbcTemplate.queryForObject("SELECT first_name FROM users WHERE id = ?", String.class, id);
    }

    private void assertThatDecryptingLegacyRowThrows(Long id) {
        try {
            userRepository.findById(id).orElseThrow().getFirstName();
            throw new AssertionError("expected decrypting a legacy plaintext row to throw — fixture isn't realistic");
        } catch (Exception expected) {
            // expected: this is exactly the crash-on-read LR-011 describes
        }
    }
}

package com.be.tools;

import com.be.config.crypto.EncryptedStringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * One-time operational tool for LR-011 — re-encrypts any legacy plaintext
 * left over in PII columns from before EncryptedStringConverter was applied
 * to them. Completely inert unless started with
 * {@code --spring.profiles.active=reencrypt-pii}; a normal app boot never
 * loads this bean.
 * <p>
 * Deliberately bypasses the JPA/Hibernate layer entirely (raw JdbcTemplate,
 * not the repositories) — going through the entities would mean
 * {@code EncryptedStringConverter.convertToEntityAttribute()} tries to
 * decrypt every row on SELECT, which throws on the very legacy plaintext
 * this tool exists to fix, crashing the read before this tool ever gets a
 * chance to inspect it. This is the same reason a normal app deploy against
 * a DB still holding plaintext in these columns would crash on first read —
 * see LR-011 in tickets.md.
 * <p>
 * Dry-run by default (logs what it WOULD change, writes nothing) — pass
 * {@code --apply} as a program argument to actually write. Idempotent:
 * already-encrypted rows are detected (a real decrypt attempt succeeds only
 * for genuine ciphertext — GCM's auth tag makes a plaintext string
 * "accidentally" decrypting successfully computationally infeasible) and
 * skipped, so running this twice — or against a DB that's already fully
 * migrated — is a no-op.
 * <p>
 * Never logs actual field values (plaintext or ciphertext) — only row
 * counts — per this project's own logging rule (CODING_PROTOCOL.md
 * forbidden patterns: no personal data in plain logs).
 * <p>
 * ⚠️ Idempotency covers crash/restart safety, NOT concurrent-write safety.
 * This tool reads a column's value, then later writes it back re-encrypted;
 * if the live app updates that same row through the normal service path in
 * between, this tool's write silently clobbers that update with a
 * re-encryption of the stale value it read earlier. Run this during a
 * maintenance window with no live traffic touching users/teachers/
 * participants — not against a table actively being written to (found by
 * architect-reviewer, 2026-08-03, before this tool was ever actually run
 * against real data — see LR-011 in tickets.md).
 */
@Component
@Profile("reencrypt-pii")
public class PiiReencryptionRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PiiReencryptionRunner.class);

    private static final Map<String, List<String>> TARGETS = Map.of(
            "users", List.of("first_name", "last_name", "phone", "address", "city", "zip_code"),
            "teachers", List.of("first_name", "last_name", "phone"),
            "participants", List.of("first_name", "last_name", "phone")
    );

    private final JdbcTemplate jdbcTemplate;
    private final EncryptedStringConverter converter;

    public PiiReencryptionRunner(JdbcTemplate jdbcTemplate, EncryptedStringConverter converter) {
        this.jdbcTemplate = jdbcTemplate;
        this.converter = converter;
    }

    @Override
    public void run(String... args) {
        boolean apply = List.of(args).contains("--apply");
        log.info("PII re-encryption tool starting. Mode: {}", apply ? "APPLY (will write changes)" : "DRY RUN (no writes)");

        int totalMigrated = 0;
        for (Map.Entry<String, List<String>> entry : TARGETS.entrySet()) {
            String table = entry.getKey();
            for (String column : entry.getValue()) {
                totalMigrated += reencryptColumn(table, column, apply);
            }
        }

        log.info("PII re-encryption tool finished. {} plaintext row(s) {} across all tracked columns.",
                totalMigrated, apply ? "migrated" : "would be migrated (re-run with --apply to write)");
    }

    private int reencryptColumn(String table, String column, boolean apply) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, " + column + " AS value FROM " + table + " WHERE " + column + " IS NOT NULL");

        int alreadyEncrypted = 0;
        int migrated = 0;

        for (Map<String, Object> row : rows) {
            Long id = ((Number) row.get("id")).longValue();
            String rawValue = (String) row.get("value");

            if (isAlreadyEncrypted(rawValue)) {
                alreadyEncrypted++;
                continue;
            }

            migrated++;
            if (apply) {
                String ciphertext = converter.convertToDatabaseColumn(rawValue);
                jdbcTemplate.update("UPDATE " + table + " SET " + column + " = ? WHERE id = ?", ciphertext, id);
            }
        }

        log.info("{}.{}: {} row(s) already encrypted, {} plaintext row(s) {}",
                table, column, alreadyEncrypted, migrated, apply ? "migrated" : "found (dry run)");
        return migrated;
    }

    /**
     * A real decrypt attempt is the only reliable test — ciphertext and
     * plaintext are otherwise indistinguishable by shape/length alone, and
     * GCM's authentication tag means a plaintext string cannot "accidentally"
     * decrypt successfully.
     */
    private boolean isAlreadyEncrypted(String value) {
        try {
            converter.convertToEntityAttribute(value);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}

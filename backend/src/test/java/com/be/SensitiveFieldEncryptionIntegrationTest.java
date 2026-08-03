package com.be;

import com.be.domain.entity.Group;
import com.be.domain.entity.Participant;
import com.be.domain.entity.Teacher;
import com.be.domain.entity.User;
import com.be.domain.entity.enums.Role;
import com.be.domain.repository.GroupRepository;
import com.be.domain.repository.ParticipantRepository;
import com.be.domain.repository.TeacherRepository;
import com.be.domain.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real end-to-end proof for the DSGVO/GoBD field-level encryption inventory
 * (2026-07-24): does @Convert(EncryptedStringConverter.class) actually work
 * through a genuine JPA persist + reload cycle, and — the part
 * EncryptedStringConverterTest's unit-level round-trip can't show — is the
 * value sitting in the real database column actually ciphertext, not
 * plaintext?
 * <p>
 * Requires JWT_SECRET and FIELD_ENCRYPTION_KEY env vars (same as every
 * other full-context test in this suite) for EncryptedStringConverter's
 * @PostConstruct to succeed.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class SensitiveFieldEncryptionIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @Transactional
    void user_sensitiveFields_roundTripThroughRealPersistence_andAreCiphertextAtRest() {
        String realIban = "DE89370400440532013000";
        String realTaxId = "12/345/67890";
        String realFirstName = "Alexander";
        String realLastName = "Musterfrau";
        String realPhone = "+49 151 23456789";
        String realAddress = "Ritterspornweg 1";
        String realCity = "Bergheim";
        String realZip = "50129";

        User user = User.builder()
                .email("encryption-audit-" + System.nanoTime() + "@example.com")
                .password("irrelevant-for-this-test")
                .firstName(realFirstName)
                .lastName(realLastName)
                .phone(realPhone)
                .address(realAddress)
                .city(realCity)
                .zipCode(realZip)
                .role(Role.USER)
                .iban(realIban)
                .taxId(realTaxId)
                .createdAt(LocalDateTime.now())
                .build();

        User saved = userRepository.save(user);

        // Force a real round trip to the DB, not the cached Java object —
        // this is exactly what the existing unit test (EncryptedStringConverterTest)
        // can never prove, since it never touches Hibernate/JPA at all.
        entityManager.flush();
        entityManager.clear();

        User reloaded = userRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getIban()).isEqualTo(realIban);
        assertThat(reloaded.getTaxId()).isEqualTo(realTaxId);
        assertThat(reloaded.getFirstName()).isEqualTo(realFirstName);
        assertThat(reloaded.getLastName()).isEqualTo(realLastName);
        assertThat(reloaded.getPhone()).isEqualTo(realPhone);
        assertThat(reloaded.getAddress()).isEqualTo(realAddress);
        assertThat(reloaded.getCity()).isEqualTo(realCity);
        assertThat(reloaded.getZipCode()).isEqualTo(realZip);

        // The real proof: read the RAW column values directly via JDBC,
        // bypassing Hibernate/the converter entirely. If @Convert weren't
        // actually wired up correctly, this would show the plaintext values.
        assertRawColumnIsCiphertext("users", "iban", saved.getId(), realIban);
        assertRawColumnIsCiphertext("users", "tax_id", saved.getId(), realTaxId);
        assertRawColumnIsCiphertext("users", "first_name", saved.getId(), realFirstName);
        assertRawColumnIsCiphertext("users", "last_name", saved.getId(), realLastName);
        assertRawColumnIsCiphertext("users", "phone", saved.getId(), realPhone);
        assertRawColumnIsCiphertext("users", "address", saved.getId(), realAddress);
        assertRawColumnIsCiphertext("users", "city", saved.getId(), realCity);
        assertRawColumnIsCiphertext("users", "zip_code", saved.getId(), realZip);

        // email is deliberately plaintext (UNIQUE constraint + login lookups)
        // — confirm it's untouched, not accidentally caught by the converter.
        String rawEmail = jdbcTemplate.queryForObject(
                "SELECT email FROM users WHERE id = ?", String.class, saved.getId());
        assertThat(rawEmail).isEqualTo(saved.getEmail());
    }

    @Test
    @Transactional
    void teacher_sensitiveFields_roundTripThroughRealPersistence_andAreCiphertextAtRest() {
        String realFirstName = "Olena";
        String realLastName = "Khudoshyna";
        String realPhone = "+49 176 00000000";

        Teacher teacher = Teacher.builder()
                .firstName(realFirstName)
                .lastName(realLastName)
                .email("teacher-audit-" + System.nanoTime() + "@example.com")
                .phone(realPhone)
                .title("Regisseurin")
                .build();

        Teacher saved = teacherRepository.save(teacher);
        entityManager.flush();
        entityManager.clear();

        Teacher reloaded = teacherRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getFirstName()).isEqualTo(realFirstName);
        assertThat(reloaded.getLastName()).isEqualTo(realLastName);
        assertThat(reloaded.getPhone()).isEqualTo(realPhone);

        assertRawColumnIsCiphertext("teachers", "first_name", saved.getId(), realFirstName);
        assertRawColumnIsCiphertext("teachers", "last_name", saved.getId(), realLastName);
        assertRawColumnIsCiphertext("teachers", "phone", saved.getId(), realPhone);

        String rawEmail = jdbcTemplate.queryForObject(
                "SELECT email FROM teachers WHERE id = ?", String.class, saved.getId());
        assertThat(rawEmail).isEqualTo(saved.getEmail());
    }

    @Test
    @Transactional
    void participant_sensitiveFields_roundTripThroughRealPersistence_andAreCiphertextAtRest() {
        // Group has no required Workshop/Activity/Teacher FK (all nullable
        // per V1__baseline.sql) — minimal valid fixture for this test only.
        Group group = Group.builder()
                .titleDe("Testgruppe")
                .titleEn("Test group")
                .titleUa("Тестова група")
                .capacity(10)
                .startDateTime(LocalDateTime.now().plusDays(1))
                .build();
        Group savedGroup = groupRepository.save(group);

        String realFirstName = "Test";
        String realLastName = "Teilnehmer";
        String realPhone = "+49 170 11111111";

        Participant participant = Participant.builder()
                .firstName(realFirstName)
                .lastName(realLastName)
                .email("participant-audit-" + System.nanoTime() + "@example.com")
                .phone(realPhone)
                .birthDate(LocalDate.of(1990, 1, 1))
                .group(savedGroup)
                .build();

        Participant saved = participantRepository.save(participant);
        entityManager.flush();
        entityManager.clear();

        Participant reloaded = participantRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getFirstName()).isEqualTo(realFirstName);
        assertThat(reloaded.getLastName()).isEqualTo(realLastName);
        assertThat(reloaded.getPhone()).isEqualTo(realPhone);

        assertRawColumnIsCiphertext("participants", "first_name", saved.getId(), realFirstName);
        assertRawColumnIsCiphertext("participants", "last_name", saved.getId(), realLastName);
        assertRawColumnIsCiphertext("participants", "phone", saved.getId(), realPhone);

        // email and birth_date are deliberately plaintext — confirm untouched.
        String rawEmail = jdbcTemplate.queryForObject(
                "SELECT email FROM participants WHERE id = ?", String.class, saved.getId());
        assertThat(rawEmail).isEqualTo(saved.getEmail());
    }

    private void assertRawColumnIsCiphertext(String table, String column, Long id, String plaintext) {
        String raw = jdbcTemplate.queryForObject(
                "SELECT " + column + " FROM " + table + " WHERE id = ?", String.class, id);
        assertThat(raw).isNotEqualTo(plaintext);
        assertThat(raw).doesNotContain(plaintext);
    }
}

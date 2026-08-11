package com.be.web.dto.request;

import com.be.domain.entity.enums.Role;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LR-012 — users/teachers/participants.{first_name,last_name,phone} (plus
 * users.{address,city,zip_code}) are encrypted-at-rest TEXT columns with no
 * DB-level length ceiling (V2__widen_encrypted_pii_columns.sql removed the
 * old VARCHAR(255) cap on purpose, to fix a different bug — but left these
 * DTOs, especially TeacherRequestDTO/ParticipantRequestDTO, with zero
 * length validation of their own). Bean Validation directly on the DTO, no
 * Spring context needed — @Valid at the controller (already present on all
 * four) is what actually wires this in at runtime.
 */
class RequestDtoValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private UserRegistrationDTO.UserRegistrationDTOBuilder validRegistration() {
        return UserRegistrationDTO.builder()
                .email("a@example.com")
                .password("password123")
                .firstName("Alice")
                .lastName("Schmidt")
                .role(Role.USER)
                .acceptedTerms(true)
                .privacyPolicyAccepted(true);
    }

    @Test
    void userRegistration_tooLongAddress_isRejected() {
        UserRegistrationDTO dto = validRegistration().address("x".repeat(256)).build();
        Set<ConstraintViolation<UserRegistrationDTO>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("address"));
    }

    @Test
    void userRegistration_addressAtLimit_isAccepted() {
        UserRegistrationDTO dto = validRegistration().address("x".repeat(255)).build();
        Set<ConstraintViolation<UserRegistrationDTO>> violations = validator.validate(dto);
        assertThat(violations).noneMatch(v -> v.getPropertyPath().toString().equals("address"));
    }

    @Test
    void userRegistration_tooLongCityOrZip_isRejected() {
        UserRegistrationDTO cityTooLong = validRegistration().city("x".repeat(101)).build();
        UserRegistrationDTO zipTooLong = validRegistration().zipCode("x".repeat(21)).build();

        assertThat(validator.validate(cityTooLong)).anyMatch(v -> v.getPropertyPath().toString().equals("city"));
        assertThat(validator.validate(zipTooLong)).anyMatch(v -> v.getPropertyPath().toString().equals("zipCode"));
    }

    @Test
    void teacherRequest_shortFirstNameOrMalformedPhone_isRejected() {
        // Before LR-012, TeacherRequestDTO had zero validation annotations —
        // both of these previously passed through untouched.
        TeacherRequestDTO shortName = TeacherRequestDTO.builder().firstName("A").lastName("Schmidt").build();
        TeacherRequestDTO badPhone = TeacherRequestDTO.builder()
                .firstName("Alice").lastName("Schmidt").phone("not-a-phone-number!!!").build();

        assertThat(validator.validate(shortName)).anyMatch(v -> v.getPropertyPath().toString().equals("firstName"));
        assertThat(validator.validate(badPhone)).anyMatch(v -> v.getPropertyPath().toString().equals("phone"));
    }

    @Test
    void teacherRequest_validValues_hasNoViolations() {
        TeacherRequestDTO dto = TeacherRequestDTO.builder()
                .firstName("Alice").lastName("Schmidt").email("a@example.com").phone("+49 30 1234567").build();

        assertThat(validator.validate(dto)).isEmpty();
    }

    // Regression: admin forms send "" for an unset optional field, not
    // null — @Pattern alone (without "^$|") rejects "" even though phone
    // isn't @NotBlank, since only an actual null is automatically valid
    // under Bean Validation, not an empty string. Found live 2026-08-11
    // building admin/teachers (LR-073) — POST /teachers 400'd on every
    // teacher with no phone number. Same fix applied to UserUpdateDTO/
    // ParticipantRequestDTO, which share the identical copy-pasted
    // pattern.
    @Test
    void teacherRequest_emptyPhone_isAccepted() {
        TeacherRequestDTO dto = TeacherRequestDTO.builder()
                .firstName("Alice").lastName("Schmidt").email("a@example.com").phone("").build();

        assertThat(validator.validate(dto)).noneMatch(v -> v.getPropertyPath().toString().equals("phone"));
    }

    @Test
    void userUpdate_emptyPhone_isAccepted() {
        UserUpdateDTO dto = UserUpdateDTO.builder().firstName("Alice").lastName("Schmidt").phone("").build();

        assertThat(validator.validate(dto)).noneMatch(v -> v.getPropertyPath().toString().equals("phone"));
    }

    @Test
    void participantRequest_emptyPhone_isAccepted() {
        ParticipantRequestDTO dto = ParticipantRequestDTO.builder()
                .firstName("Alice").lastName("Schmidt").phone("").build();

        assertThat(validator.validate(dto)).noneMatch(v -> v.getPropertyPath().toString().equals("phone"));
    }

    @Test
    void participantRequest_shortLastNameOrTooLongPhone_isRejected() {
        ParticipantRequestDTO shortName = ParticipantRequestDTO.builder().firstName("Alice").lastName("X").build();
        ParticipantRequestDTO longPhone = ParticipantRequestDTO.builder()
                .firstName("Alice").lastName("Schmidt").phone("+" + "1".repeat(30)).build();

        assertThat(validator.validate(shortName)).anyMatch(v -> v.getPropertyPath().toString().equals("lastName"));
        assertThat(validator.validate(longPhone)).anyMatch(v -> v.getPropertyPath().toString().equals("phone"));
    }
}

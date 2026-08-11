package com.be.web.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherRequestDTO {
    // LR-012 — teachers.{first_name,last_name,phone} are encrypted at rest
    // (V2__widen_encrypted_pii_columns.sql widened them to TEXT precisely
    // because they'd previously had no length ceiling at all) — this DTO
    // had zero validation on any field, the same gap User's DTOs already
    // had before this ticket.
    @Size(min = 2, max = 50)
    private String firstName;

    @Size(min = 2, max = 50)
    private String lastName;

    @Email
    private String email;

    // Empty string must be allowed alongside the phone shape — phone is
    // optional (nullable in the DB), and admin forms send "" for a blank
    // field, not null. Found live 2026-08-11 building admin/teachers
    // (LR-073): @Pattern without the "^$|" alternative rejects "" even
    // though it isn't @NotBlank, since Bean Validation only treats an
    // actual null as automatically valid, not an empty string.
    @Pattern(regexp = "^$|\\+?[0-9\\s\\-()]+")
    @Size(max = 25)
    private String phone;

    private String title;
    private boolean approved;
    private String bioDe;
    private String bioEn;
    private String bioUa;
    private boolean active;
}
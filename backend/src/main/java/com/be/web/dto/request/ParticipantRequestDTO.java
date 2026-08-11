package com.be.web.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantRequestDTO {
    // LR-012 — same gap as TeacherRequestDTO: participants.{first_name,
    // last_name,phone} are encrypted/unbounded TEXT, this DTO had zero
    // validation on any field.
    @Size(min = 2, max = 50)
    private String firstName;

    @Size(min = 2, max = 50)
    private String lastName;

    @Email
    private String email;

    // Same "^$|" fix as TeacherRequestDTO.phone — see its comment.
    @Pattern(regexp = "^$|\\+?[0-9\\s\\-()]+")
    @Size(max = 25)
    private String phone;

    private LocalDate birthDate;
    private Long groupId;
    private boolean active;
}
package com.be.web.dto.request;

import com.be.domain.entity.enums.Role;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegistrationDTO {
    @NotBlank(message = "Email is mandatory")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password is mandatory")
    @Size(min = 8, message = "Password should be at least 8 characters long")
    private String password;

    @NotBlank(message = "First name is mandatory")
    @Size(min = 2, max = 50, message = "First name should be between 2 and 50 characters long")
    private String firstName;

    @NotBlank(message = "Last name is mandatory")
    @Size(min = 2, max = 50, message = "Last name should be between 2 and 50 characters long")
    private String lastName;

    @Pattern(regexp = "\\+[0-9\\s\\-()]+", message = "Invalid phone number format")
    private String phone;

    @Past(message = "Birth date should be in the past")
    private LocalDate birthDate;

    @Builder.Default
    private Role role = Role.USER;

    @AssertTrue(message = "You must accept terms and conditions")
    @Builder.Default
    private boolean acceptedTerms = false;

    @AssertTrue(message = "You must accept privacy policy")
    @Builder.Default
    private boolean privacyPolicyAccepted = false;

    private String address;
    private String city;
    private String zipCode;
    private String country;
}

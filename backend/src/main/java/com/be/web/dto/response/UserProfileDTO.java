package com.be.web.dto.response;

import com.be.domain.entity.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private LocalDate birthDate;
    private Role role;

    // Address for billing
    private String address;
    private String city;
    private String zipCode;
    private String country;

    // For teachers
    private String title;
    private String bio;
    private String iban;
    private String taxId;

    // Account status
    private Boolean enabled;
    private Boolean emailVerified;

    // GDPR information
    private LocalDateTime termsAcceptedAt;
    private LocalDateTime privacyPolicyAcceptedAt;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Statistics (для dashboard)
    private Integer totalOrders;
    private Integer activeParticipants;
    private Boolean hasTeacherProfile;
}
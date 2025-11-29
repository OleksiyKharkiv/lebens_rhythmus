package com.be.web.dto.request;

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
public class UserUpdateDTO {

    @Size(min = 2, max = 50)
    private String firstName;

    @Size(min = 2, max = 50)
    private String lastName;

    @Pattern(regexp = "\\+?[0-9\\s\\-()]+")
    private String phone;

    private LocalDate birthDate;

    // Address information
    private String address;
    private String city;
    private String zipCode;
    private String country;

    // Teacher-specific fields
    private String title;
    private String bio;
}
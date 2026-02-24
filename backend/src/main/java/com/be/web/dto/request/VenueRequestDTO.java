package com.be.web.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VenueRequestDTO {
    private String name;
    private String address;
    private String city;
    private String postalCode;
    private String country;
    private Integer capacity;
    private String description;
    private String contactPhone;
    private String contactEmail;
}
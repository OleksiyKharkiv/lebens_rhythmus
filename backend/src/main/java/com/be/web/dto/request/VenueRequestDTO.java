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
    // LR-015 — one physical room = one Venue row (e.g. "TLab29" + room
    // "Blauer Saal"), so two rooms in the same building are two rows
    // sharing name/address, distinguished by this field.
    private String room;
    private String address;
    private String city;
    private String postalCode;
    private String country;
    private Integer capacity;
    private String description;
    private String contactPhone;
    private String contactEmail;
}
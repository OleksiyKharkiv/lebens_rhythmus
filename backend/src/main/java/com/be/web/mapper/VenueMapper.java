package com.be.web.mapper;

import com.be.domain.entity.Venue;
import com.be.web.dto.request.VenueRequestDTO;
import com.be.web.dto.response.VenueResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class VenueMapper {

    public VenueResponseDTO toResponseDTO(Venue venue) {
        if (venue == null) return null;
        return VenueResponseDTO.builder()
                .id(venue.getId())
                .name(venue.getName())
                .address(venue.getAddress())
                .city(venue.getCity())
                .postalCode(venue.getPostalCode())
                .country(venue.getCountry())
                .capacity(venue.getCapacity())
                .description(venue.getDescription())
                .contactPhone(venue.getContactPhone())
                .contactEmail(venue.getContactEmail())
                .createdAt(venue.getCreatedAt())
                .updatedAt(venue.getUpdatedAt())
                .build();
    }

    public Venue fromRequestDTO(VenueRequestDTO dto) {
        if (dto == null) return null;
        return Venue.builder()
                .name(dto.getName())
                .address(dto.getAddress())
                .city(dto.getCity())
                .postalCode(dto.getPostalCode())
                .country(dto.getCountry())
                .capacity(dto.getCapacity())
                .description(dto.getDescription())
                .contactPhone(dto.getContactPhone())
                .contactEmail(dto.getContactEmail())
                .build();
    }
}
package com.be.web.controller;

import com.be.domain.entity.Venue;
import com.be.service.VenueService;
import com.be.web.dto.request.VenueRequestDTO;
import com.be.web.dto.response.VenueResponseDTO;
import com.be.web.mapper.VenueMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/venues")
public class VenueController {

    private final VenueService venueService;
    private final VenueMapper venueMapper;

    public VenueController(VenueService venueService, VenueMapper venueMapper) {
        this.venueService = venueService;
        this.venueMapper = venueMapper;
    }

    @GetMapping
    public ResponseEntity<List<VenueResponseDTO>> getAll() {
        List<Venue> venues = venueService.getAll();
        return ResponseEntity.ok(venues.stream()
                .map(venueMapper::toResponseDTO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VenueResponseDTO> getById(@PathVariable Long id) {
        Venue venue = venueService.getById(id);
        return ResponseEntity.ok(venueMapper.toResponseDTO(venue));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER')")
    public ResponseEntity<VenueResponseDTO> create(@Valid @RequestBody VenueRequestDTO dto) {
        Venue created = venueService.create(dto);
        return ResponseEntity.status(201).body(venueMapper.toResponseDTO(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER')")
    public ResponseEntity<VenueResponseDTO> update(@PathVariable Long id, @Valid @RequestBody VenueRequestDTO dto) {
        Venue updated = venueService.update(id, dto);
        return ResponseEntity.ok(venueMapper.toResponseDTO(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        venueService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

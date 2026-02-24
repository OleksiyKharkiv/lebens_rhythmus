package com.be.web.controller;

import com.be.domain.entity.Performance;
import com.be.service.PerformanceService;
import com.be.web.dto.request.PerformanceRequestDTO;
import com.be.web.dto.response.PerformanceResponseDTO;
import com.be.web.mapper.PerformanceMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/performances")
public class PerformanceController {

    private final PerformanceService performanceService;
    private final PerformanceMapper performanceMapper;

    public PerformanceController(PerformanceService performanceService, PerformanceMapper performanceMapper) {
        this.performanceService = performanceService;
        this.performanceMapper = performanceMapper;
    }

    @GetMapping
    public ResponseEntity<List<PerformanceResponseDTO>> getAll() {
        List<Performance> performances = performanceService.getAll();
        return ResponseEntity.ok(performances.stream()
                .map(performanceMapper::toResponseDTO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PerformanceResponseDTO> getById(@PathVariable Long id) {
        Performance performance = performanceService.getById(id);
        return ResponseEntity.ok(performanceMapper.toResponseDTO(performance));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER')")
    public ResponseEntity<PerformanceResponseDTO> create(@Valid @RequestBody PerformanceRequestDTO dto) {
        Performance created = performanceService.create(dto);
        return ResponseEntity.status(201).body(performanceMapper.toResponseDTO(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER')")
    public ResponseEntity<PerformanceResponseDTO> update(@PathVariable Long id, @Valid @RequestBody PerformanceRequestDTO dto) {
        Performance updated = performanceService.update(id, dto);
        return ResponseEntity.ok(performanceMapper.toResponseDTO(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        performanceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
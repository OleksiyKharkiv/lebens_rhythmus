package com.be.web.controller;

import com.be.domain.entity.AgeGroup;
import com.be.service.AgeGroupService;
import com.be.web.dto.request.AgeGroupRequestDTO;
import com.be.web.dto.response.AgeGroupResponseDTO;
import com.be.web.mapper.AgeGroupMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/age-groups")
public class AgeGroupController {

    private final AgeGroupService ageGroupService;
    private final AgeGroupMapper ageGroupMapper;

    public AgeGroupController(AgeGroupService ageGroupService, AgeGroupMapper ageGroupMapper) {
        this.ageGroupService = ageGroupService;
        this.ageGroupMapper = ageGroupMapper;
    }

    @GetMapping
    public ResponseEntity<List<AgeGroupResponseDTO>> getAll() {
        List<AgeGroup> groups = ageGroupService.getAll();
        return ResponseEntity.ok(groups.stream()
                .map(ageGroupMapper::toResponseDTO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AgeGroupResponseDTO> getById(@PathVariable Long id) {
        AgeGroup group = ageGroupService.getById(id);
        return ResponseEntity.ok(ageGroupMapper.toResponseDTO(group));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER')")
    public ResponseEntity<AgeGroupResponseDTO> create(@Valid @RequestBody AgeGroupRequestDTO dto) {
        AgeGroup created = ageGroupService.create(dto);
        return ResponseEntity.status(201).body(ageGroupMapper.toResponseDTO(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER')")
    public ResponseEntity<AgeGroupResponseDTO> update(@PathVariable Long id, @Valid @RequestBody AgeGroupRequestDTO dto) {
        AgeGroup updated = ageGroupService.update(id, dto);
        return ResponseEntity.ok(ageGroupMapper.toResponseDTO(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ageGroupService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

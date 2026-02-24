package com.be.web.controller;

import com.be.domain.entity.Language;
import com.be.service.LanguageService;
import com.be.web.dto.request.LanguageRequestDTO;
import com.be.web.dto.response.LanguageResponseDTO;
import com.be.web.mapper.LanguageMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/languages")
public class LanguageController {

    private final LanguageService languageService;
    private final LanguageMapper languageMapper;

    public LanguageController(LanguageService languageService, LanguageMapper languageMapper) {
        this.languageService = languageService;
        this.languageMapper = languageMapper;
    }

    @GetMapping
    public ResponseEntity<List<LanguageResponseDTO>> getAll() {
        List<Language> languages = languageService.getAll();
        return ResponseEntity.ok(languages.stream()
                .map(languageMapper::toResponseDTO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LanguageResponseDTO> getById(@PathVariable Long id) {
        Language language = languageService.getById(id);
        return ResponseEntity.ok(languageMapper.toResponseDTO(language));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER')")
    public ResponseEntity<LanguageResponseDTO> create(@Valid @RequestBody LanguageRequestDTO dto) {
        Language created = languageService.create(dto);
        return ResponseEntity.status(201).body(languageMapper.toResponseDTO(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER')")
    public ResponseEntity<LanguageResponseDTO> update(@PathVariable Long id, @Valid @RequestBody LanguageRequestDTO dto) {
        Language updated = languageService.update(id, dto);
        return ResponseEntity.ok(languageMapper.toResponseDTO(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        languageService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
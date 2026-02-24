package com.be.web.controller;

import com.be.domain.entity.File;
import com.be.service.FileService;
import com.be.web.dto.request.FileRequestDTO;
import com.be.web.dto.response.FileResponseDTO;
import com.be.web.mapper.FileMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileService fileService;
    private final FileMapper fileMapper;

    public FileController(FileService fileService, FileMapper fileMapper) {
        this.fileService = fileService;
        this.fileMapper = fileMapper;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER')")
    public ResponseEntity<List<FileResponseDTO>> getAll() {
        List<File> files = fileService.getAll();
        return ResponseEntity.ok(files.stream()
                .map(fileMapper::toResponseDTO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER')")
    public ResponseEntity<FileResponseDTO> getById(@PathVariable Long id) {
        File file = fileService.getById(id);
        return ResponseEntity.ok(fileMapper.toResponseDTO(file));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER')")
    public ResponseEntity<FileResponseDTO> create(@Valid @RequestBody FileRequestDTO dto) {
        File created = fileService.create(dto);
        return ResponseEntity.status(201).body(fileMapper.toResponseDTO(created));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        fileService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
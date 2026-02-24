package com.be.web.controller;

import com.be.domain.entity.Participant;
import com.be.service.ParticipantService;
import com.be.web.dto.request.ParticipantRequestDTO;
import com.be.web.dto.response.ParticipantResponseDTO;
import com.be.web.mapper.ParticipantMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/participants")
public class ParticipantController {

    private final ParticipantService participantService;
    private final ParticipantMapper participantMapper;

    public ParticipantController(ParticipantService participantService, ParticipantMapper participantMapper) {
        this.participantService = participantService;
        this.participantMapper = participantMapper;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER') or hasRole('TEACHER')")
    public ResponseEntity<List<ParticipantResponseDTO>> getAll() {
        List<Participant> participants = participantService.getAll();
        return ResponseEntity.ok(participants.stream()
                .map(participantMapper::toResponseDTO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER') or hasRole('TEACHER')")
    public ResponseEntity<ParticipantResponseDTO> getById(@PathVariable Long id) {
        Participant participant = participantService.getById(id);
        return ResponseEntity.ok(participantMapper.toResponseDTO(participant));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER')")
    public ResponseEntity<ParticipantResponseDTO> create(@Valid @RequestBody ParticipantRequestDTO dto) {
        Participant created = participantService.create(dto);
        return ResponseEntity.status(201).body(participantMapper.toResponseDTO(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER')")
    public ResponseEntity<ParticipantResponseDTO> update(@PathVariable Long id, @Valid @RequestBody ParticipantRequestDTO dto) {
        Participant updated = participantService.update(id, dto);
        return ResponseEntity.ok(participantMapper.toResponseDTO(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        participantService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
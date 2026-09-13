package com.be.web.controller;

import com.be.config.JwtAuthUtils;
import com.be.domain.entity.Participant;
import com.be.service.ParticipantService;
import com.be.service.TeacherService;
import com.be.web.dto.request.ParticipantRequestDTO;
import com.be.web.dto.response.ParticipantResponseDTO;
import com.be.web.mapper.ParticipantMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/participants")
public class ParticipantController {

    private final ParticipantService participantService;
    private final ParticipantMapper participantMapper;
    private final TeacherService teacherService;

    public ParticipantController(ParticipantService participantService, ParticipantMapper participantMapper,
                                  TeacherService teacherService) {
        this.participantService = participantService;
        this.participantMapper = participantMapper;
        this.teacherService = teacherService;
    }

    // Security check Tier 1.1 (2026-09-13) — role-only check used to be
    // the whole story here, same bug class LR-024 already fixed on
    // WorkshopController.byTeacher/GroupController.getGroupsByTeacher/
    // EnrollmentController.participantsForGroup: any TEACHER account
    // could list every participant of every group, not just their own —
    // Participant carries a child's encrypted name/email/phone/birthDate
    // (see Participant.java), so this was a real cross-teacher PII leak,
    // not a business-config one. ADMIN/BUSINESS_OWNER still see everyone,
    // unchanged; a caller whose real role is TEACHER is now scoped to
    // their own groups' participants.
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER') or hasRole('TEACHER')")
    public ResponseEntity<List<ParticipantResponseDTO>> getAll(@AuthenticationPrincipal Jwt jwt) {
        List<Participant> participants;
        if (JwtAuthUtils.hasRole(jwt, "TEACHER")) {
            Long callerTeacherId = teacherService.resolveTeacherIdForUser(JwtAuthUtils.extractUserId(jwt))
                    .orElseThrow(() -> new AccessDeniedException("No teacher profile linked to this account"));
            participants = participantService.getAllForTeacher(callerTeacherId);
        } else {
            participants = participantService.getAll();
        }
        return ResponseEntity.ok(participants.stream()
                .map(participantMapper::toResponseDTO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER') or hasRole('TEACHER')")
    public ResponseEntity<ParticipantResponseDTO> getById(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        Participant participant = participantService.getById(id);
        if (JwtAuthUtils.hasRole(jwt, "TEACHER")) {
            Long callerTeacherId = teacherService.resolveTeacherIdForUser(JwtAuthUtils.extractUserId(jwt))
                    .orElseThrow(() -> new AccessDeniedException("No teacher profile linked to this account"));
            boolean isOwnGroup = participant.getGroup() != null && participant.getGroup().getTeacher() != null
                    && callerTeacherId.equals(participant.getGroup().getTeacher().getId());
            if (!isOwnGroup) {
                throw new AccessDeniedException("Cannot view another teacher's participant");
            }
        }
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
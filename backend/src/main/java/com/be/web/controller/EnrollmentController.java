package com.be.web.controller;

import com.be.domain.entity.Enrollment;
import com.be.service.EnrollmentService;
import com.be.web.dto.request.EnrollmentRequestDTO;
import com.be.web.dto.response.EnrollmentAdminDTO;
import com.be.web.dto.response.EnrollmentResponseDTO;
import com.be.web.mapper.EnrollmentMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import static com.be.config.JwtAuthUtils.extractUserId;
import static com.be.config.JwtAuthUtils.hasRole;

@RestController
@RequestMapping("/api/v1")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final EnrollmentMapper mapper;

    public EnrollmentController(EnrollmentService enrollmentService, EnrollmentMapper mapper) {
        this.enrollmentService = enrollmentService;
        this.mapper = mapper;
    }

    /**
     * Enroll current user into a workshop.
     */
    @PostMapping("/workshops/{workshopId}/enroll")
    @PreAuthorize("""
                hasRole('USER') or hasRole('TEACHER')
                or hasRole('BUSINESS_OWNER') or hasRole('ADMIN')
            """)
    public ResponseEntity<EnrollmentResponseDTO> enroll(
            @PathVariable Long workshopId,
            @Valid @RequestBody(required = false) EnrollmentRequestDTO request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = extractUserId(jwt);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        Enrollment e = enrollmentService.enroll(workshopId, userId, request);
        EnrollmentResponseDTO dto = mapper.toResponseDTO(e);

        return ResponseEntity
                .created(URI.create("/api/v1/enrollments/" + e.getId()))
                .body(dto);
    }

    /**
     * Get current user's enrollments.
     */
    @GetMapping("/users/me/enrollments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EnrollmentResponseDTO>> myEnrollments(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = extractUserId(jwt);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        List<EnrollmentResponseDTO> dto = enrollmentService.getByUser(userId)
                .stream()
                .map(mapper::toResponseDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dto);
    }

    /**
     * Cancel enrollment.
     * User can cancel own enrollment.
     * Admin / business owner can cancel any.
     */
    @DeleteMapping("/enrollments/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> cancelEnrollment(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = extractUserId(jwt);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        boolean isPrivileged =
                hasRole(jwt, "ADMIN") || hasRole(jwt, "BUSINESS_OWNER");

        enrollmentService.cancelEnrollment(id, userId, isPrivileged);
        return ResponseEntity.ok().build();
    }

    /**
     * Admin / business owner: list participants of workshop.
     */
    @GetMapping("/admin/workshops/{workshopId}/participants")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER')")
    public ResponseEntity<List<EnrollmentAdminDTO>> participantsForWorkshop(
            @PathVariable Long workshopId
    ) {
        List<EnrollmentAdminDTO> dto = enrollmentService.getByWorkshop(workshopId)
                .stream()
                .map(mapper::toAdminDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dto);
    }

    /**
     * Teacher / admin: list participants of group.
     */
    @GetMapping("/teacher/groups/{groupId}/participants")
    @PreAuthorize("hasRole('TEACHER') or hasRole('BUSINESS_OWNER') or hasRole('ADMIN')")
    public ResponseEntity<List<EnrollmentAdminDTO>> participantsForGroup(
            @PathVariable Long groupId
    ) {
        List<EnrollmentAdminDTO> dto = enrollmentService.getByGroup(groupId)
                .stream()
                .map(mapper::toAdminDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dto);
    }
}

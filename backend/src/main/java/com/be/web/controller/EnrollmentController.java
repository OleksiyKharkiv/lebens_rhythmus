package com.be.web.controller;

import com.be.domain.entity.Enrollment;
import com.be.domain.entity.User;
import com.be.service.EnrollmentService;
import com.be.web.dto.request.EnrollmentRequestDTO;
import com.be.web.dto.response.EnrollmentAdminDTO;
import com.be.web.dto.response.EnrollmentResponseDTO;
import com.be.web.mapper.EnrollmentMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

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
     * Enroll the current user into a workshop (optionally into a group).
     */
    @PostMapping("/workshops/{workshopId}/enroll")
    @PreAuthorize("hasRole('USER') or hasRole('TEACHER') or hasRole('BUSINESS_OWNER') or hasRole('ADMIN')")
    public ResponseEntity<EnrollmentResponseDTO> enroll(@PathVariable Long workshopId,
                                                        @Valid @RequestBody(required = false) EnrollmentRequestDTO request,
                                                        @AuthenticationPrincipal User currentUser) {
        Enrollment e = enrollmentService.enroll(workshopId, currentUser.getId(), request);
        EnrollmentResponseDTO dto = mapper.toResponseDTO(e);
        return ResponseEntity.created(URI.create("/api/v1/enrollments/" + e.getId())).body(dto);
    }

    /**
     * Get current user's enrollments.
     */
    @GetMapping("/users/me/enrollments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EnrollmentResponseDTO>> myEnrollments(@AuthenticationPrincipal User currentUser) {
        List<Enrollment> list = enrollmentService.getByUser(currentUser.getId());
        List<EnrollmentResponseDTO> dto = list.stream().map(mapper::toResponseDTO).collect(Collectors.toList());
        return ResponseEntity.ok(dto);
    }

    /**
     * Cancel enrollment (user can cancel own; admin/business owner can cancel any).
     */
    @DeleteMapping("/enrollments/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> cancelEnrollment(@PathVariable Long id, @AuthenticationPrincipal User currentUser) {
        boolean isAdmin = currentUser.getRole() != null && currentUser.getRole().name().equals("ADMIN");
        enrollmentService.cancelEnrollment(id, currentUser.getId(), isAdmin);
        return ResponseEntity.ok().build();
    }

    /**
     * Admin: list participants for workshop
     */
    @GetMapping("/admin/workshops/{workshopId}/participants")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER')")
    public ResponseEntity<List<EnrollmentAdminDTO>> participantsForWorkshop(@PathVariable Long workshopId) {
        List<Enrollment> list = enrollmentService.getByWorkshop(workshopId);
        List<EnrollmentAdminDTO> dto = list.stream().map(mapper::toAdminDTO).collect(Collectors.toList());
        return ResponseEntity.ok(dto);
    }

    /**
     * Teacher or admin: list participants for a group
     */
    @GetMapping("/teacher/groups/{groupId}/participants")
    @PreAuthorize("hasRole('TEACHER') or hasRole('BUSINESS_OWNER') or hasRole('ADMIN')")
    public ResponseEntity<List<EnrollmentAdminDTO>> participantsForGroup(@PathVariable Long groupId) {
        List<Enrollment> list = enrollmentService.getByGroup(groupId);
        List<EnrollmentAdminDTO> dto = list.stream().map(mapper::toAdminDTO).collect(Collectors.toList());
        return ResponseEntity.ok(dto);
    }
}
package com.be.web.controller;

import com.be.domain.entity.Enrollment;
import com.be.domain.entity.Group;
import com.be.service.EnrollmentService;
import com.be.service.GroupService;
import com.be.service.TeacherService;
import com.be.web.dto.request.EnrollmentRequestDTO;
import com.be.web.dto.response.EnrollmentAdminDTO;
import com.be.web.dto.response.EnrollmentResponseDTO;
import com.be.web.mapper.EnrollmentMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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
    private final GroupService groupService;
    private final TeacherService teacherService;

    public EnrollmentController(EnrollmentService enrollmentService, EnrollmentMapper mapper,
                                 GroupService groupService, TeacherService teacherService) {
        this.enrollmentService = enrollmentService;
        this.mapper = mapper;
        this.groupService = groupService;
        this.teacherService = teacherService;
    }

    /**
     * Enroll the current user into a workshop.
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
     * Enroll the current user into a course. LR-084.
     */
    @PostMapping("/courses/{courseId}/enroll")
    @PreAuthorize("""
                hasRole('USER') or hasRole('TEACHER')
                or hasRole('BUSINESS_OWNER') or hasRole('ADMIN')
            """)
    public ResponseEntity<EnrollmentResponseDTO> enrollCourse(
            @PathVariable Long courseId,
            @Valid @RequestBody(required = false) EnrollmentRequestDTO request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = extractUserId(jwt);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        Enrollment e = enrollmentService.enrollCourse(courseId, userId, request);
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
     * User can cancel their own enrollment.
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
     * Admin / business owner: list participants of the workshop.
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
     * Teacher / admin: list participants of the group.
     * <p>
     * LR-024 — role-only check used to be the whole story: any TEACHER
     * account could pass any groupId and read another teacher's
     * participants (children's name/email — EnrollmentAdminDTO carries
     * full UserBasicDTO per row). ADMIN/BUSINESS_OWNER still see every
     * group, unchanged; a caller whose real role is TEACHER now must
     * actually be the group's assigned teacher.
     */
    @GetMapping("/teacher/groups/{groupId}/participants")
    @PreAuthorize("hasRole('TEACHER') or hasRole('BUSINESS_OWNER') or hasRole('ADMIN')")
    public ResponseEntity<List<EnrollmentAdminDTO>> participantsForGroup(
            @PathVariable Long groupId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        if (hasRole(jwt, "TEACHER")) {
            Long callerTeacherId = teacherService.resolveTeacherIdForUser(extractUserId(jwt))
                    .orElseThrow(() -> new AccessDeniedException("No teacher profile linked to this account"));
            Group group = groupService.findById(groupId);
            boolean isOwnGroup = group.getTeacher() != null && callerTeacherId.equals(group.getTeacher().getId());
            if (!isOwnGroup) {
                throw new AccessDeniedException("Cannot view another teacher's group participants");
            }
        }

        List<EnrollmentAdminDTO> dto = enrollmentService.getByGroup(groupId)
                .stream()
                .map(mapper::toAdminDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dto);
    }
}
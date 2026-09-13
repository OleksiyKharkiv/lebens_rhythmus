package com.be.web.controller;

import com.be.config.JwtAuthUtils;
import com.be.domain.entity.Teacher;
import com.be.service.TeacherService;
import com.be.web.dto.TeacherInfoDTO;
import com.be.web.dto.request.TeacherRequestDTO;
import com.be.web.mapper.TeacherMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/teachers")
public class TeacherController {

    private final TeacherService teacherService;
    private final TeacherMapper teacherMapper;

    public TeacherController(TeacherService teacherService, TeacherMapper teacherMapper) {
        this.teacherService = teacherService;
        this.teacherMapper = teacherMapper;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER')")
    public ResponseEntity<List<TeacherInfoDTO>> getAll() {
        List<Teacher> teachers = teacherService.getAll();
        return ResponseEntity.ok(teachers.stream()
                .map(teacherMapper::toInfoDTO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER')")
    public ResponseEntity<TeacherInfoDTO> getById(@PathVariable Long id) {
        Teacher teacher = teacherService.getById(id);
        return ResponseEntity.ok(teacherMapper.toInfoDTO(teacher));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ADMIN') or hasRole('BUSINESS_OWNER')")
    public ResponseEntity<TeacherInfoDTO> getMe(@AuthenticationPrincipal Jwt jwt) {
        Long userId = JwtAuthUtils.extractUserId(jwt);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        return teacherService.resolveTeacherIdForUser(userId)
                .map(teacherService::getById)
                .map(teacherMapper::toInfoDTO)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER')")
    public ResponseEntity<TeacherInfoDTO> create(@Valid @RequestBody TeacherRequestDTO dto) {
        Teacher created = teacherService.create(dto);
        return ResponseEntity.status(201).body(teacherMapper.toInfoDTO(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER')")
    public ResponseEntity<TeacherInfoDTO> update(@PathVariable Long id, @Valid @RequestBody TeacherRequestDTO dto) {
        Teacher updated = teacherService.update(id, dto);
        return ResponseEntity.ok(teacherMapper.toInfoDTO(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        teacherService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
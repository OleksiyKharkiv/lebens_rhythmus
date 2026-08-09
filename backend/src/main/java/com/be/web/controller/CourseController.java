package com.be.web.controller;

import com.be.config.JwtAuthUtils;
import com.be.domain.entity.Course;
import com.be.service.CourseService;
import com.be.web.dto.request.CourseCreateDTO;
import com.be.web.dto.response.CourseDetailDTO;
import com.be.web.dto.response.CourseListDTO;
import com.be.web.mapper.CourseMapper;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Course REST endpoints — mirrors WorkshopController's shape (public
 * list/detail, ADMIN/BUSINESS_OWNER write). Course itself carries no
 * schedule (LR-ADR-023) — see GroupController for Course-linked Group
 * scheduling once LR-081 ships.
 */
@RestController
@RequestMapping("/api/v1/courses")
public class CourseController {

    private static final Logger log = LoggerFactory.getLogger(CourseController.class);

    private final CourseService courseService;
    private final CourseMapper mapper;

    public CourseController(CourseService courseService, CourseMapper mapper) {
        this.courseService = courseService;
        this.mapper = mapper;
    }

    @GetMapping
    public ResponseEntity<List<CourseListDTO>> list() {
        List<Course> list = courseService.listCourses();
        List<CourseListDTO> dto = list.stream().map(mapper::toListDTO).collect(Collectors.toList());
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseDetailDTO> getDetail(@PathVariable Long id) {
        Course c = courseService.getById(id);
        return ResponseEntity.ok(mapper.toDetailDTO(c));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER')")
    public ResponseEntity<CourseDetailDTO> create(@Valid @RequestBody CourseCreateDTO dto,
                                                   @AuthenticationPrincipal Jwt jwt) {
        Long requesterId = JwtAuthUtils.extractUserId(jwt);
        log.debug("create course requested by userId={}", requesterId);

        Course created = courseService.createCourse(dto);
        return ResponseEntity.status(201).body(mapper.toDetailDTO(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER')")
    public ResponseEntity<CourseDetailDTO> update(@PathVariable Long id,
                                                   @Valid @RequestBody CourseCreateDTO dto,
                                                   @AuthenticationPrincipal Jwt jwt) {
        Long requesterId = JwtAuthUtils.extractUserId(jwt);
        log.debug("update course {} requested by userId={}", id, requesterId);

        Course updated = courseService.updateCourse(id, dto);
        return ResponseEntity.ok(mapper.toDetailDTO(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER')")
    public ResponseEntity<?> delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        Long requesterId = JwtAuthUtils.extractUserId(jwt);
        log.debug("delete course {} requested by userId={}", id, requesterId);

        courseService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }
}

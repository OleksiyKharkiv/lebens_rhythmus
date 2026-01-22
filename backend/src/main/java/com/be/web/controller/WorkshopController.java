package com.be.web.controller;

import com.be.config.JwtAuthUtils;
import com.be.domain.entity.Workshop;
import com.be.service.WorkshopService;
import com.be.web.dto.request.WorkshopCreateDTO;
import com.be.web.dto.response.WorkshopDetailDTO;
import com.be.web.dto.response.WorkshopListDTO;
import com.be.web.mapper.WorkshopMapper;
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
 * Workshop REST endpoints — controller uses Jwt principal for optional auditing.
 */
@RestController
@RequestMapping("/api/v1/workshops")
public class WorkshopController {

    private static final Logger log = LoggerFactory.getLogger(WorkshopController.class);

    private final WorkshopService workshopService;
    private final WorkshopMapper mapper;

    public WorkshopController(WorkshopService workshopService, WorkshopMapper mapper) {
        this.workshopService = workshopService;
        this.mapper = mapper;
    }

    // Public list (optional filter upcoming=true)
    @GetMapping
    public ResponseEntity<List<WorkshopListDTO>> list(@RequestParam(required = false, defaultValue = "false") boolean upcoming,
                                                      @RequestParam(required = false) String q) {
        List<Workshop> list = workshopService.listWorkshops(upcoming);
        if (q != null && !q.isBlank()) {
            list = list.stream()
                    .filter(w -> w.getWorkshopName() != null && w.getWorkshopName().toLowerCase().contains(q.toLowerCase()))
                    .toList();
        }
        List<WorkshopListDTO> dto = list.stream().map(mapper::toListDTO).collect(Collectors.toList());
        return ResponseEntity.ok(dto);
    }

    // Detail
    @GetMapping("/{id}")
    public ResponseEntity<WorkshopDetailDTO> getDetail(@PathVariable Long id) {
        Workshop w = workshopService.getById(id);
        WorkshopDetailDTO dto = mapper.toDetailDTO(w);
        return ResponseEntity.ok(dto);
    }

    // Create — admin or business owner
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER')")
    public ResponseEntity<WorkshopDetailDTO> create(@Valid @RequestBody WorkshopCreateDTO dto,
                                                    @AuthenticationPrincipal Jwt jwt) {
        // extract requester id for logging / audit
        Long requesterId = JwtAuthUtils.extractUserId(jwt);
        log.debug("create workshop requested by userId={}", requesterId);

        Workshop created = workshopService.createWorkshop(dto);
        return ResponseEntity.status(201).body(mapper.toDetailDTO(created));
    }

    // Update — admin or business owner
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER')")
    public ResponseEntity<WorkshopDetailDTO> update(@PathVariable Long id,
                                                    @Valid @RequestBody WorkshopCreateDTO dto,
                                                    @AuthenticationPrincipal Jwt jwt) {
        Long requesterId = JwtAuthUtils.extractUserId(jwt);
        log.debug("update workshop {} requested by userId={}", id, requesterId);

        Workshop updated = workshopService.updateWorkshop(id, dto);
        return ResponseEntity.ok(mapper.toDetailDTO(updated));
    }

    // (Optional) delete signature can also accept Jwt if you want to audit who deleted.
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER')")
    public ResponseEntity<?> delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        Long requesterId = JwtAuthUtils.extractUserId(jwt);
        log.debug("delete workshop {} requested by userId={}", id, requesterId);

        workshopService.deleteWorkshop(id);
        return ResponseEntity.noContent().build();
    }

    // teacher view of own workshops
    @GetMapping("/teacher/{teacherId}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('BUSINESS_OWNER') or hasRole('ADMIN')")
    public ResponseEntity<List<WorkshopListDTO>> byTeacher(@PathVariable Long teacherId) {
        List<Workshop> list = workshopService.findByTeacher(teacherId);
        return ResponseEntity.ok(list.stream().map(mapper::toListDTO).collect(Collectors.toList()));
    }

    // NOTE: helper methods were moved to JwtAuthUtils to avoid duplication across controllers
}
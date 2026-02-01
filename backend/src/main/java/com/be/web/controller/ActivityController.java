package com.be.web.controller;

import com.be.domain.entity.Activity;
import com.be.service.ActivityService;
import com.be.web.dto.request.ActivityRequestDTO;
import com.be.web.dto.response.ActivityResponseDTO;
import com.be.web.mapper.ActivityMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/activities")
public class ActivityController {

    private final ActivityService activityService;
    private final ActivityMapper activityMapper;

    public ActivityController(ActivityService activityService, ActivityMapper activityMapper) {
        this.activityService = activityService;
        this.activityMapper = activityMapper;
    }

    @GetMapping
    public ResponseEntity<List<ActivityResponseDTO>> getAll() {
        List<Activity> activities = activityService.getAllActivities();
        return ResponseEntity.ok(activities.stream()
                .map(activityMapper::toResponseDTO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ActivityResponseDTO> getById(@PathVariable Long id) {
        Activity activity = activityService.getActivityById(id);
        return ResponseEntity.ok(activityMapper.toResponseDTO(activity));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER')")
    public ResponseEntity<ActivityResponseDTO> create(@Valid @RequestBody ActivityRequestDTO dto) {
        Activity created = activityService.createActivity(dto);
        return ResponseEntity.status(201).body(activityMapper.toResponseDTO(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER')")
    public ResponseEntity<ActivityResponseDTO> update(@PathVariable Long id, @Valid @RequestBody ActivityRequestDTO dto) {
        Activity updated = activityService.updateActivity(id, dto);
        return ResponseEntity.ok(activityMapper.toResponseDTO(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        activityService.deleteActivity(id);
        return ResponseEntity.noContent().build();
    }
}

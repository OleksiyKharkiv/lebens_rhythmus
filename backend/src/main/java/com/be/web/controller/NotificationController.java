package com.be.web.controller;

import com.be.domain.entity.Notification;
import com.be.service.NotificationCRUDService;
import com.be.web.dto.request.NotificationRequestDTO;
import com.be.web.dto.response.NotificationResponseDTO;
import com.be.web.mapper.NotificationMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationCRUDService notificationService;
    private final NotificationMapper notificationMapper;

    public NotificationController(NotificationCRUDService notificationService, NotificationMapper notificationMapper) {
        this.notificationService = notificationService;
        this.notificationMapper = notificationMapper;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<NotificationResponseDTO>> getAll() {
        List<Notification> notifications = notificationService.getAll();
        return ResponseEntity.ok(notifications.stream()
                .map(notificationMapper::toResponseDTO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NotificationResponseDTO> getById(@PathVariable Long id) {
        Notification notification = notificationService.getById(id);
        return ResponseEntity.ok(notificationMapper.toResponseDTO(notification));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NotificationResponseDTO> create(@Valid @RequestBody NotificationRequestDTO dto) {
        Notification created = notificationService.create(dto);
        return ResponseEntity.status(201).body(notificationMapper.toResponseDTO(created));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        notificationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
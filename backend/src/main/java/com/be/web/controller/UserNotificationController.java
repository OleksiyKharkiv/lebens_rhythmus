package com.be.web.controller;

import com.be.config.JwtAuthUtils;
import com.be.domain.entity.UserNotification;
import com.be.service.UserNotificationService;
import com.be.web.dto.response.UserNotificationResponseDTO;
import com.be.web.mapper.UserNotificationMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/user-notifications")
public class UserNotificationController {

    private final UserNotificationService userNotificationService;
    private final UserNotificationMapper userNotificationMapper;

    public UserNotificationController(UserNotificationService userNotificationService, UserNotificationMapper userNotificationMapper) {
        this.userNotificationService = userNotificationService;
        this.userNotificationMapper = userNotificationMapper;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UserNotificationResponseDTO>> getMyNotifications(@AuthenticationPrincipal Jwt jwt) {
        Long userId = JwtAuthUtils.extractUserId(jwt);
        List<UserNotification> list = userNotificationService.getByUserId(userId);
        return ResponseEntity.ok(list.stream()
                .map(userNotificationMapper::toResponseDTO)
                .collect(Collectors.toList()));
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserNotificationResponseDTO> markAsRead(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        // LR-089 (security audit, 2026-09-07) — this used to extract userId
        // and then never check it against anything: any authenticated user
        // could mark ANY other user's notification as read by iterating
        // {id}, and the response leaked that person's userId/username/
        // notificationTitle back to the caller. Same ownership-check shape
        // as OrderController.getById.
        UserNotification notification = userNotificationService.getById(id);
        Long userId = JwtAuthUtils.extractUserId(jwt);
        boolean isAdmin = JwtAuthUtils.hasRole(jwt, "ADMIN") || JwtAuthUtils.hasRole(jwt, "BUSINESS_OWNER");
        if (!isAdmin && !notification.getUser().getId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }

        UserNotification updated = userNotificationService.markAsRead(id);
        return ResponseEntity.ok(userNotificationMapper.toResponseDTO(updated));
    }
}
package com.be.web.controller;

import com.be.config.JwtAuthUtils;
import com.be.domain.entity.Feedback;
import com.be.service.FeedbackService;
import com.be.web.dto.request.FeedbackRequestDTO;
import com.be.web.dto.response.FeedbackResponseDTO;
import com.be.web.mapper.FeedbackMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/feedbacks")
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final FeedbackMapper feedbackMapper;

    public FeedbackController(FeedbackService feedbackService, FeedbackMapper feedbackMapper) {
        this.feedbackService = feedbackService;
        this.feedbackMapper = feedbackMapper;
    }

    @GetMapping
    public ResponseEntity<List<FeedbackResponseDTO>> getAll() {
        List<Feedback> feedbacks = feedbackService.getAll();
        return ResponseEntity.ok(feedbacks.stream()
                .map(feedbackMapper::toResponseDTO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FeedbackResponseDTO> getById(@PathVariable Long id) {
        Feedback feedback = feedbackService.getById(id);
        return ResponseEntity.ok(feedbackMapper.toResponseDTO(feedback));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FeedbackResponseDTO> create(@Valid @RequestBody FeedbackRequestDTO dto,
                                                      @AuthenticationPrincipal Jwt jwt) {
        Long userId = JwtAuthUtils.extractUserId(jwt);
        Feedback created = feedbackService.create(dto, userId);
        return ResponseEntity.status(201).body(feedbackMapper.toResponseDTO(created));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        feedbackService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
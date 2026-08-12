package com.be.web.controller;

import com.be.domain.entity.Session;
import com.be.service.SessionService;
import com.be.web.dto.request.SessionWriteDTO;
import com.be.web.dto.response.SessionDTO;
import com.be.web.mapper.SessionMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Session is a child of Group (LR-ADR-022, LR-067) — every route here is
 * scoped under a Group, there is no standalone /sessions resource
 * (matches SessionService's own shape). New in LR-074 — SessionService
 * existed since LR-067 with no controller on top of it yet.
 */
@RestController
@RequestMapping("/api/v1/groups/{groupId}/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;
    private final SessionMapper sessionMapper;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<SessionDTO> getSessions(@PathVariable Long groupId) {
        List<Session> sessions = sessionService.findByGroupId(groupId);
        return sessions.stream().map(sessionMapper::toDto).collect(Collectors.toList());
    }

    // Replaces the whole day-list in one call — matches the admin form
    // shape (resubmits every day each time, not incremental add/remove),
    // same convention as GroupController/WorkshopController writes.
    @PutMapping
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER')")
    public List<SessionDTO> replaceSessions(@PathVariable Long groupId, @Valid @RequestBody List<SessionWriteDTO> body) {
        List<SessionService.SessionInput> inputs = body.stream()
                .map(dto -> new SessionService.SessionInput(dto.getStartDateTime(), dto.getEndDateTime(), dto.getVenueId()))
                .collect(Collectors.toList());
        List<Session> sessions = sessionService.replaceSessionsForGroup(groupId, inputs);
        return sessions.stream().map(sessionMapper::toDto).collect(Collectors.toList());
    }
}

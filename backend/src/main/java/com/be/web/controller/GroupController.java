package com.be.web.controller;

import com.be.config.JwtAuthUtils;
import com.be.domain.entity.Group;
import com.be.service.GroupService;
import com.be.service.TeacherService;
import com.be.web.dto.request.GroupCreateDTO;
import com.be.web.dto.request.GroupUpdateDTO;
import com.be.web.dto.response.GroupDTO;
import com.be.web.mapper.GroupMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;
    private final GroupMapper groupMapper;
    private final TeacherService teacherService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<GroupDTO> getAllGroups(@RequestParam(required = false) Long workshopId,
                                        @RequestParam(required = false) Long courseId) {
        List<Group> groups;
        if (workshopId != null) {
            groups = groupService.findByWorkshopId(workshopId);
        } else if (courseId != null) {
            groups = groupService.findByCourseId(courseId);
        } else {
            groups = groupService.findAll();
        }

        return groups.stream()
                .map(groupMapper::toDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public GroupDTO getGroupById(@PathVariable Long id) {
        Group g = groupService.findById(id);
        return groupMapper.toDto(g);
    }

    // LR-030 — was @RequestBody Group (raw entity, mass-assignment risk),
    // now an explicit DTO — see GroupCreateDTO/GroupService.createGroup.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER')")
    public GroupDTO createGroup(@Valid @RequestBody GroupCreateDTO dto) {
        Group created = groupService.createGroup(dto);
        return groupMapper.toDto(created);
    }

    // Artefact-audit 2026-08-14 — was @RequestBody Group (raw entity, same
    // mass-assignment class as LR-030's createGroup fix), now GroupUpdateDTO.
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER')")
    public GroupDTO updateGroup(@PathVariable Long id, @Valid @RequestBody GroupUpdateDTO dto) {
        Group updated = groupService.update(id, dto);
        return groupMapper.toDto(updated);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER')")
    public void deleteGroup(@PathVariable Long id) {
        groupService.deleteById(id);
    }

    @GetMapping("/activity/{activityId}")
    @ResponseStatus(HttpStatus.OK)
    public List<GroupDTO> getGroupsByActivity(@PathVariable Long activityId) {
        List<Group> groups = groupService.findByActivityId(activityId);
        return groups.stream().map(groupMapper::toDto).collect(Collectors.toList());
    }

    // LR-024 — had NO @PreAuthorize at all (any authenticated USER could
    // call this, same bug class LR-006 already fixed on this controller's
    // write methods) + role-only ownership, same fix as
    // WorkshopController.byTeacher.
    @GetMapping("/teacher/{teacherId}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('TEACHER') or hasRole('BUSINESS_OWNER') or hasRole('ADMIN')")
    public List<GroupDTO> getGroupsByTeacher(@PathVariable Long teacherId, @AuthenticationPrincipal Jwt jwt) {
        if (JwtAuthUtils.hasRole(jwt, "TEACHER")) {
            Long callerTeacherId = teacherService.resolveTeacherIdForUser(JwtAuthUtils.extractUserId(jwt))
                    .orElseThrow(() -> new AccessDeniedException("No teacher profile linked to this account"));
            if (!callerTeacherId.equals(teacherId)) {
                throw new AccessDeniedException("Cannot view another teacher's groups");
            }
        }
        List<Group> groups = groupService.findByTeacherId(teacherId);
        return groups.stream().map(groupMapper::toDto).collect(Collectors.toList());
    }
}
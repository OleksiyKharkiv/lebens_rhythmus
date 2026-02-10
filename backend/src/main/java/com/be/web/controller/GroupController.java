package com.be.web.controller;

import com.be.domain.entity.Group;
import com.be.service.GroupService;
import com.be.web.dto.response.GroupDTO;
import com.be.web.mapper.GroupMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;
    private final GroupMapper groupMapper;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<GroupDTO> getAllGroups(@RequestParam(required = false) Long workshopId) {
        List<Group> groups = (workshopId != null)
                ? groupService.findByWorkshopId(workshopId)
                : groupService.findAll();

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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupDTO createGroup(@RequestBody Group group) {
        Group created = groupService.save(group);
        return groupMapper.toDto(created);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public GroupDTO updateGroup(@PathVariable Long id, @RequestBody Group group) {
        group.setId(id);
        Group updated = groupService.update(group);
        return groupMapper.toDto(updated);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGroup(@PathVariable Long id) {
        groupService.deleteById(id);
    }

    @GetMapping("/activity/{activityId}")
    @ResponseStatus(HttpStatus.OK)
    public List<GroupDTO> getGroupsByActivity(@PathVariable Long activityId) {
        List<Group> groups = groupService.findByActivityId(activityId);
        return groups.stream().map(groupMapper::toDto).collect(Collectors.toList());
    }

    @GetMapping("/teacher/{teacherId}")
    @ResponseStatus(HttpStatus.OK)
    public List<GroupDTO> getGroupsByTeacher(@PathVariable Long teacherId) {
        List<Group> groups = groupService.findByTeacherId(teacherId);
        return groups.stream().map(groupMapper::toDto).collect(Collectors.toList());
    }
}
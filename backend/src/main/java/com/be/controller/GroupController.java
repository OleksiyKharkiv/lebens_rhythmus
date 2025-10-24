package com.be.controller;

import com.be.domain.entity.Group;
import com.be.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<Group> getAllGroups() {
        return groupService.findAll();
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Group getGroupById(@PathVariable Long id) {
        return groupService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Group createGroup(@RequestBody Group group) {
        return groupService.save(group);
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Group updateGroup(@PathVariable Long id, @RequestBody Group group) {
        group.setId(id);
        return groupService.update(group);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGroup(@PathVariable Long id) {
        groupService.deleteById(id);
    }

    @GetMapping("/activity/{activityId}")
    @ResponseStatus(HttpStatus.OK)
    public List<Group> getGroupsByActivity(@PathVariable Long activityId) {
        return groupService.findByActivityId(activityId);
    }

    @GetMapping("/teacher/{teacherId}")
    @ResponseStatus(HttpStatus.OK)
    public List<Group> getGroupsByTeacher(@PathVariable Long teacherId) {
        return groupService.findByTeacherId(teacherId);
    }
}

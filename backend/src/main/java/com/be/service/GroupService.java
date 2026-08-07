package com.be.service;

import com.be.domain.entity.Activity;
import com.be.domain.entity.AgeGroup;
import com.be.domain.entity.Group;
import com.be.domain.entity.Participant;
import com.be.domain.entity.Teacher;
import com.be.domain.entity.Venue;
import com.be.domain.entity.Workshop;
import com.be.domain.repository.ActivityRepository;
import com.be.domain.repository.AgeGroupRepository;
import com.be.domain.repository.GroupRepository;
import com.be.domain.repository.TeacherRepository;
import com.be.domain.repository.VenueRepository;
import com.be.domain.repository.WorkshopRepository;
import com.be.web.dto.request.GroupCreateDTO;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GroupService {
    private final GroupRepository groupRepository;
    private final WorkshopRepository workshopRepository;
    private final TeacherRepository teacherRepository;
    private final ActivityRepository activityRepository;
    private final VenueRepository venueRepository;
    private final AgeGroupRepository ageGroupRepository;

    @Transactional(readOnly = true)
    public List<Group> findAll() {
        return groupRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Group> findActiveGroups() {
        return groupRepository.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    public Group findById(Long id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Group not found with id: " + id));
    }

    // LR-030 — replaces the old bare save(Group) pass-through, which bound
    // the raw JPA entity as @RequestBody with no field allow-list at all
    // (a caller could set capacityLeft directly, or reference an existing
    // Enrollment's id inside a crafted `enrollments` array to re-parent it
    // — Group.enrollments is CascadeType.ALL, orphanRemoval=true). Explicit
    // fields only, ids resolved here — same pattern already used in
    // WorkshopService.createWorkshop.
    @Transactional
    public Group createGroup(GroupCreateDTO dto) {
        Group group = Group.builder()
                .titleDe(dto.getTitleDe())
                .titleEn(dto.getTitleEn())
                .titleUa(dto.getTitleUa())
                .capacity(dto.getCapacity() != null ? dto.getCapacity() : 0)
                .startDateTime(dto.getStartDateTime())
                .endDateTime(dto.getEndDateTime())
                .active(dto.isActive())
                .build();

        if (dto.getWorkshopId() != null) {
            Workshop workshop = workshopRepository.findById(dto.getWorkshopId())
                    .orElseThrow(() -> new EntityNotFoundException("Workshop not found with id: " + dto.getWorkshopId()));
            group.setWorkshop(workshop);
        }
        if (dto.getTeacherId() != null) {
            Teacher teacher = teacherRepository.findById(dto.getTeacherId())
                    .orElseThrow(() -> new EntityNotFoundException("Teacher not found with id: " + dto.getTeacherId()));
            group.setTeacher(teacher);
        }
        if (dto.getActivityId() != null) {
            Activity activity = activityRepository.findById(dto.getActivityId())
                    .orElseThrow(() -> new EntityNotFoundException("Activity not found with id: " + dto.getActivityId()));
            group.setActivity(activity);
        }
        if (dto.getVenueId() != null) {
            Venue venue = venueRepository.findById(dto.getVenueId())
                    .orElseThrow(() -> new EntityNotFoundException("Venue not found with id: " + dto.getVenueId()));
            group.setVenue(venue);
        }
        if (dto.getAgeGroupId() != null) {
            AgeGroup ageGroup = ageGroupRepository.findById(dto.getAgeGroupId())
                    .orElseThrow(() -> new EntityNotFoundException("Age group not found with id: " + dto.getAgeGroupId()));
            group.setAgeGroup(ageGroup);
        }

        return groupRepository.save(group);
    }

    /**
     * Updates group properties then persists changes
     */
    @Transactional
    public Group update(Group group) {
        Group existingGroup = findById(group.getId());

        existingGroup.setTitleDe(group.getTitleDe());
        existingGroup.setTitleEn(group.getTitleEn());
        existingGroup.setTitleUa(group.getTitleUa());
        existingGroup.setCapacity(group.getCapacity());
        // startDateTime/endDateTime were missing here entirely — editing a
        // group's schedule silently had no effect (found while building the
        // admin Groups page, LR-008). `workshop` reassignment on edit is
        // deliberately NOT added here — that's a bigger decision (does
        // moving a group to a different workshop need to touch existing
        // enrollments?) than a same-shape field copy, left for a separate
        // ticket rather than decided here.
        existingGroup.setStartDateTime(group.getStartDateTime());
        existingGroup.setEndDateTime(group.getEndDateTime());
        existingGroup.setActivity(group.getActivity());
        existingGroup.setAgeGroup(group.getAgeGroup());
        existingGroup.setLanguage(group.getLanguage());
        existingGroup.setTeacher(group.getTeacher());
        // LR-015 — was missing entirely, unlike the other relations right
        // above it: editing an existing group's venue silently no-op'd on
        // save (found while wiring the admin Groups venue select).
        existingGroup.setVenue(group.getVenue());
        existingGroup.setActive(group.isActive());

        return groupRepository.save(existingGroup);
    }

    @Transactional
    public void deleteById(Long id) {
        Group group = findById(id);
        groupRepository.delete(group);
    }

    @Transactional
    public void deactivateGroup(Long id) {
        Group group = findById(id);
        group.setActive(false);
        groupRepository.save(group);
    }

    /**
     * Adds participant to the group if capacity allows
     */
    @Transactional
    public boolean addParticipant(Long groupId, Participant participant) {
        Group group = findById(groupId);
        Set<Participant> participants = group.getParticipants();

        if (participants.size() >= group.getCapacity()) {
            return false;
        }

        participants.add(participant);
        groupRepository.save(group);
        return true;
    }

    @Transactional
    public void removeParticipant(Long groupId, Participant participant) {
        Group group = findById(groupId);
        group.getParticipants().remove(participant);
        groupRepository.save(group);
    }

    @Transactional(readOnly = true)
    public boolean hasAvailableSpots(Long groupId) {
        Group group = findById(groupId);
        return group.getParticipants().size() < group.getCapacity();
    }

    @Transactional(readOnly = true)
    public List<Group> findByActivityId(Long activityId) {
        Activity activity = Activity.builder().id(activityId).build();
        return groupRepository.findByActivity(activity);
    }

    @Transactional(readOnly = true)
    public List<Group> findByTeacherId(Long teacherId) {
        Teacher teacher = Teacher.builder().id(teacherId).build();
        return groupRepository.findByTeacher(teacher);
    }

    @Transactional(readOnly = true)
    public List<Group> findByWorkshopId(Long workshopId) {
        return groupRepository.findByWorkshopId(workshopId);
    }
}
package com.be.service;

import com.be.domain.entity.Activity;
import com.be.domain.entity.Group;
import com.be.domain.entity.Participant;
import com.be.domain.entity.Teacher;
import com.be.domain.repository.GroupRepository;
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

    @Transactional
    public Group save(Group group) {
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
        existingGroup.setActivity(group.getActivity());
        existingGroup.setAgeGroup(group.getAgeGroup());
        existingGroup.setLanguage(group.getLanguage());
        existingGroup.setTeacher(group.getTeacher());
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
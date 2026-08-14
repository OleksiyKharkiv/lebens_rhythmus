package com.be.service;

import com.be.domain.entity.Activity;
import com.be.domain.entity.AgeGroup;
import com.be.domain.entity.Course;
import com.be.domain.entity.Group;
import com.be.domain.entity.Language;
import com.be.domain.entity.Participant;
import com.be.domain.entity.Teacher;
import com.be.domain.entity.Venue;
import com.be.domain.entity.Workshop;
import com.be.domain.repository.ActivityRepository;
import com.be.domain.repository.AgeGroupRepository;
import com.be.domain.repository.CourseRepository;
import com.be.domain.repository.GroupRepository;
import com.be.domain.repository.LanguageRepository;
import com.be.domain.repository.TeacherRepository;
import com.be.domain.repository.VenueRepository;
import com.be.domain.repository.WorkshopRepository;
import com.be.web.dto.request.GroupCreateDTO;
import com.be.web.dto.request.GroupUpdateDTO;
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
    private final CourseRepository courseRepository;
    private final LanguageRepository languageRepository;

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
        // LR-081 (LR-ADR-023) — workshopId/courseId are mutually
        // exclusive; a Group belongs to at most one of them. Unenforced
        // at the DB level (same as Workshop.courseId/Performance.courseId,
        // LR-ADR-021), so it must be checked here.
        if (dto.getWorkshopId() != null && dto.getCourseId() != null) {
            throw new IllegalArgumentException("Group cannot be linked to both a Workshop and a Course");
        }

        Group group = Group.builder()
                .titleDe(dto.getTitleDe())
                .titleEn(dto.getTitleEn())
                .titleUa(dto.getTitleUa())
                .capacity(dto.getCapacity() != null ? dto.getCapacity() : 0)
                .startDateTime(dto.getStartDateTime())
                .endDateTime(dto.getEndDateTime())
                .active(dto.isActive())
                .recurrenceDays(dto.getRecurrenceDays())
                .recurrenceStartDate(dto.getRecurrenceStartDate())
                .recurrenceEndDate(dto.getRecurrenceEndDate())
                .build();

        if (dto.getWorkshopId() != null) {
            Workshop workshop = workshopRepository.findById(dto.getWorkshopId())
                    .orElseThrow(() -> new EntityNotFoundException("Workshop not found with id: " + dto.getWorkshopId()));
            group.setWorkshop(workshop);
        }
        if (dto.getCourseId() != null) {
            Course course = courseRepository.findById(dto.getCourseId())
                    .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + dto.getCourseId()));
            group.setCourse(course);
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
     * Updates group properties then persists changes.
     * <p>
     * Artefact-audit 2026-08-14 — replaced the old {@code update(Group group)}
     * that bound {@code @RequestBody Group} directly (the last raw-entity gap
     * on this controller; createGroup was already fixed this way, LR-030).
     * A crafted body could set {@code capacityLeft} directly, or reference an
     * existing Enrollment's id inside a crafted {@code enrollments} array to
     * re-parent it onto this group ({@code Group.enrollments} is
     * {@code CascadeType.ALL}, {@code orphanRemoval=true}). No workshopId —
     * group -&gt; workshop reassignment was never supported here, unaffected.
     */
    @Transactional
    public Group update(Long id, GroupUpdateDTO dto) {
        Group existingGroup = findById(id);

        Course course = null;
        if (dto.getCourseId() != null) {
            course = courseRepository.findById(dto.getCourseId())
                    .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + dto.getCourseId()));
        }
        // LR-081 (LR-ADR-023) — same guard as createGroup; workshop
        // reassignment isn't supported here (see method javadoc above), so
        // the only way both could end up set is linking a course onto a
        // group that already belongs to a workshop.
        if (existingGroup.getWorkshop() != null && course != null) {
            throw new IllegalArgumentException("Group cannot be linked to both a Workshop and a Course");
        }

        existingGroup.setTitleDe(dto.getTitleDe());
        existingGroup.setTitleEn(dto.getTitleEn());
        existingGroup.setTitleUa(dto.getTitleUa());
        existingGroup.setCapacity(dto.getCapacity() != null ? dto.getCapacity() : existingGroup.getCapacity());
        existingGroup.setStartDateTime(dto.getStartDateTime());
        existingGroup.setEndDateTime(dto.getEndDateTime());
        existingGroup.setActive(dto.isActive());
        existingGroup.setCourse(course);
        existingGroup.setRecurrenceDays(dto.getRecurrenceDays());
        existingGroup.setRecurrenceStartDate(dto.getRecurrenceStartDate());
        existingGroup.setRecurrenceEndDate(dto.getRecurrenceEndDate());

        // Authoritative on every field below (id present -> resolve and set,
        // absent -> clear) — NOT skip-if-null. Skip-if-null was the exact bug
        // found live in CourseService/WorkshopService this same session
        // (teacher/ageGroup silently un-removable on update); same fix here.
        if (dto.getActivityId() != null) {
            Activity activity = activityRepository.findById(dto.getActivityId())
                    .orElseThrow(() -> new EntityNotFoundException("Activity not found with id: " + dto.getActivityId()));
            existingGroup.setActivity(activity);
        } else {
            existingGroup.setActivity(null);
        }
        if (dto.getAgeGroupId() != null) {
            AgeGroup ageGroup = ageGroupRepository.findById(dto.getAgeGroupId())
                    .orElseThrow(() -> new EntityNotFoundException("Age group not found with id: " + dto.getAgeGroupId()));
            existingGroup.setAgeGroup(ageGroup);
        } else {
            existingGroup.setAgeGroup(null);
        }
        if (dto.getLanguageId() != null) {
            Language language = languageRepository.findById(dto.getLanguageId())
                    .orElseThrow(() -> new EntityNotFoundException("Language not found with id: " + dto.getLanguageId()));
            existingGroup.setLanguage(language);
        } else {
            existingGroup.setLanguage(null);
        }
        if (dto.getTeacherId() != null) {
            Teacher teacher = teacherRepository.findById(dto.getTeacherId())
                    .orElseThrow(() -> new EntityNotFoundException("Teacher not found with id: " + dto.getTeacherId()));
            existingGroup.setTeacher(teacher);
        } else {
            existingGroup.setTeacher(null);
        }
        if (dto.getVenueId() != null) {
            Venue venue = venueRepository.findById(dto.getVenueId())
                    .orElseThrow(() -> new EntityNotFoundException("Venue not found with id: " + dto.getVenueId()));
            existingGroup.setVenue(venue);
        } else {
            existingGroup.setVenue(null);
        }

        return groupRepository.save(existingGroup);
    }

    @Transactional(readOnly = true)
    public List<Group> findByCourseId(Long courseId) {
        return groupRepository.findByCourseId(courseId);
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
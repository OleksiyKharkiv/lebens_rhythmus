package com.be.service;

import com.be.domain.entity.AgeGroup;
import com.be.domain.entity.Course;
import com.be.domain.entity.Group;
import com.be.domain.entity.User;
import com.be.domain.entity.enums.CourseStatus;
import com.be.domain.repository.AgeGroupRepository;
import com.be.domain.repository.CourseRepository;
import com.be.domain.repository.GroupRepository;
import com.be.domain.repository.UserRepository;
import com.be.web.dto.request.CourseCreateDTO;
import com.be.web.mapper.CourseMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional
public class CourseService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final AgeGroupRepository ageGroupRepository;
    private final GroupRepository groupRepository;
    private final CourseMapper courseMapper;

    public CourseService(CourseRepository courseRepository,
                          UserRepository userRepository,
                          AgeGroupRepository ageGroupRepository,
                          GroupRepository groupRepository,
                          CourseMapper courseMapper) {
        this.courseRepository = Objects.requireNonNull(courseRepository, "courseRepository");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.ageGroupRepository = Objects.requireNonNull(ageGroupRepository, "ageGroupRepository");
        this.groupRepository = Objects.requireNonNull(groupRepository, "groupRepository");
        this.courseMapper = Objects.requireNonNull(courseMapper, "courseMapper");
    }

    // 2026-08-14 — the public course page needs the linked Group's
    // schedule (start/end/weekdays) to compute total session count itself;
    // "one Course = one Group" MVP scope (LR-081), first match if several.
    @Transactional(readOnly = true)
    public Optional<Group> findScheduleGroup(Long courseId) {
        List<Group> groups = groupRepository.findByCourseId(courseId);
        return groups.isEmpty() ? Optional.empty() : Optional.of(groups.get(0));
    }

    @Transactional(readOnly = true)
    public List<Course> listCourses() {
        return courseRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Course getById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found with id: " + id));
    }

    @Transactional
    public Course createCourse(CourseCreateDTO dto) {
        Course c = courseMapper.fromCreateDTO(dto);
        applyAgeGroupAndTeacher(c, dto);
        return courseRepository.save(c);
    }

    @Transactional
    public Course updateCourse(Long id, CourseCreateDTO dto) {
        Course existing = getById(id);

        if (dto.getTitleDe() != null) existing.setTitleDe(dto.getTitleDe());
        if (dto.getTitleEn() != null) existing.setTitleEn(dto.getTitleEn());
        if (dto.getTitleUa() != null) existing.setTitleUa(dto.getTitleUa());
        if (dto.getDescriptionDe() != null) existing.setDescriptionDe(dto.getDescriptionDe());
        if (dto.getDescriptionEn() != null) existing.setDescriptionEn(dto.getDescriptionEn());
        if (dto.getDescriptionUa() != null) existing.setDescriptionUa(dto.getDescriptionUa());
        if (dto.getIsOnline() != null) existing.setOnline(dto.getIsOnline());
        if (dto.getIsSynchronous() != null) existing.setSynchronous(dto.getIsSynchronous());
        if (dto.getHasRecordings() != null) existing.setHasRecordings(dto.getHasRecordings());
        if (dto.getFormatDisclaimerDe() != null) existing.setFormatDisclaimerDe(dto.getFormatDisclaimerDe());
        if (dto.getFormatDisclaimerEn() != null) existing.setFormatDisclaimerEn(dto.getFormatDisclaimerEn());
        if (dto.getFormatDisclaimerUa() != null) existing.setFormatDisclaimerUa(dto.getFormatDisclaimerUa());

        // Urgent ticket 2026-08-14 — authoritative, NOT skip-if-null: these
        // three are genuinely optional/clearable via the admin form (e.g. a
        // number input emptied sends null). Skip-if-null here would repeat
        // the exact bug class documented in docs/context/KNOWN_ISSUES.md
        // (found live 5x already this project on optional-FK fields — same
        // root cause applies to plain nullable value fields, not just FKs).
        existing.setPrice(dto.getPrice());
        existing.setPriceDescription(dto.getPriceDescription());
        existing.setBackgroundImageUrl(dto.getBackgroundImageUrl());
        // status is different: the admin <select> always carries a real
        // value, never cleared to "nothing" — skip-if-null is correct here,
        // same as WorkshopService.updateWorkshop's status handling.
        if (dto.getStatus() != null) existing.setStatus(CourseStatus.valueOf(dto.getStatus()));

        applyAgeGroupAndTeacher(existing, dto);

        return courseRepository.save(existing);
    }

    @Transactional
    public void deleteCourse(Long id) {
        if (!courseRepository.existsById(id)) {
            throw new RuntimeException("Course not found: " + id);
        }
        courseRepository.deleteById(id);
    }

    // Both relations are authoritative on every update, not skip-if-null:
    // the admin form always submits the whole current state (PUT, not
    // PATCH), so a null here means "explicitly cleared in the UI", not
    // "field omitted" — unlike title/description, which are always
    // non-null in practice because the form never lets them go empty.
    private void applyAgeGroupAndTeacher(Course c, CourseCreateDTO dto) {
        if (dto.getAgeGroupId() != null) {
            AgeGroup ageGroup = ageGroupRepository.findById(dto.getAgeGroupId())
                    .orElseThrow(() -> new RuntimeException("Age group not found: " + dto.getAgeGroupId()));
            c.setAgeGroup(ageGroup);
        } else {
            c.setAgeGroup(null);
        }
        if (dto.getTeacherId() != null) {
            User teacher = userRepository.findById(dto.getTeacherId())
                    .orElseThrow(() -> new RuntimeException("Teacher user not found: " + dto.getTeacherId()));
            c.setTeacher(teacher);
        } else {
            c.setTeacher(null);
        }
    }
}

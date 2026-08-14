package com.be.service;

import com.be.domain.entity.*;
import com.be.domain.repository.*;
import com.be.web.dto.request.GroupCreateDTO;
import com.be.web.dto.request.GroupUpdateDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * LR-030 — createGroup() used to bind the raw JPA entity directly
 * (@RequestBody Group), with the service doing a bare
 * groupRepository.save(group) — no field allow-list, so a crafted body
 * could set capacityLeft directly or reference an existing enrollment's
 * id to re-parent it (Group.enrollments is CascadeType.ALL,
 * orphanRemoval=true). GroupCreateDTO structurally has no capacityLeft/
 * enrollments field at all — not just "not copied", genuinely
 * unrepresentable in the request shape. This test covers the new
 * resolution logic (ids -> real entities), the actual new behavior
 * worth a regression guard.
 */
@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private GroupRepository groupRepository;
    @Mock
    private WorkshopRepository workshopRepository;
    @Mock
    private TeacherRepository teacherRepository;
    @Mock
    private ActivityRepository activityRepository;
    @Mock
    private VenueRepository venueRepository;
    @Mock
    private AgeGroupRepository ageGroupRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private LanguageRepository languageRepository;

    private GroupService service() {
        return new GroupService(groupRepository, workshopRepository, teacherRepository,
                activityRepository, venueRepository, ageGroupRepository, courseRepository, languageRepository);
    }

    @Test
    void createGroup_resolvesAllReferencedIdsToRealEntities() {
        Workshop workshop = Workshop.builder().id(1L).build();
        Teacher teacher = Teacher.builder().id(2L).build();
        Activity activity = Activity.builder().id(3L).build();
        Venue venue = Venue.builder().id(4L).build();
        AgeGroup ageGroup = AgeGroup.builder().id(5L).build();

        when(workshopRepository.findById(1L)).thenReturn(Optional.of(workshop));
        when(teacherRepository.findById(2L)).thenReturn(Optional.of(teacher));
        when(activityRepository.findById(3L)).thenReturn(Optional.of(activity));
        when(venueRepository.findById(4L)).thenReturn(Optional.of(venue));
        when(ageGroupRepository.findById(5L)).thenReturn(Optional.of(ageGroup));
        when(groupRepository.save(any(Group.class))).thenAnswer(inv -> inv.getArgument(0));

        GroupCreateDTO dto = GroupCreateDTO.builder()
                .titleDe("Kinder Tanz").titleEn("Kids Dance").titleUa("Дитячий танець")
                .capacity(12)
                .startDateTime(LocalDateTime.now().plusDays(1))
                .workshopId(1L).teacherId(2L).activityId(3L).venueId(4L).ageGroupId(5L)
                .active(true)
                .build();

        Group created = service().createGroup(dto);

        assertThat(created.getWorkshop()).isEqualTo(workshop);
        assertThat(created.getTeacher()).isEqualTo(teacher);
        assertThat(created.getActivity()).isEqualTo(activity);
        assertThat(created.getVenue()).isEqualTo(venue);
        assertThat(created.getAgeGroup()).isEqualTo(ageGroup);
        assertThat(created.getCapacity()).isEqualTo(12);
    }

    // LR-081 (LR-ADR-023) — a Group cannot be linked to both a Workshop
    // and a Course at once; unenforced at the DB level, so this is the
    // one place that actually blocks it.
    @Test
    void createGroup_withBothWorkshopIdAndCourseId_throws() {
        GroupCreateDTO dto = GroupCreateDTO.builder()
                .titleDe("X").titleEn("X").titleUa("X")
                .capacity(5)
                .startDateTime(LocalDateTime.now().plusDays(1))
                .workshopId(1L).courseId(9L)
                .active(true)
                .build();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service().createGroup(dto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createGroup_allRelationsOptional_noneSetWhenIdsAreNull() {
        when(groupRepository.save(any(Group.class))).thenAnswer(inv -> inv.getArgument(0));

        GroupCreateDTO dto = GroupCreateDTO.builder()
                .titleDe("Solo").titleEn("Solo").titleUa("Solo")
                .capacity(5)
                .startDateTime(LocalDateTime.now().plusDays(1))
                .active(true)
                .build();

        Group created = service().createGroup(dto);

        assertThat(created.getWorkshop()).isNull();
        assertThat(created.getTeacher()).isNull();
        assertThat(created.getActivity()).isNull();
        assertThat(created.getVenue()).isNull();
        assertThat(created.getAgeGroup()).isNull();
    }

    /**
     * Artefact-audit 2026-08-14 — update() used to bind {@code @RequestBody
     * Group} directly (the last raw-entity gap on this controller); these
     * cover the new GroupUpdateDTO-based resolution logic, mirroring the
     * createGroup tests above.
     */
    @Test
    void update_resolvesAllReferencedIdsToRealEntities() {
        Group existing = Group.builder().id(1L).titleDe("Old").titleEn("Old").titleUa("Old").capacity(5).build();
        Teacher teacher = Teacher.builder().id(2L).build();
        Activity activity = Activity.builder().id(3L).build();
        Venue venue = Venue.builder().id(4L).build();
        AgeGroup ageGroup = AgeGroup.builder().id(5L).build();
        Language language = Language.builder().id(6L).build();

        when(groupRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(teacherRepository.findById(2L)).thenReturn(Optional.of(teacher));
        when(activityRepository.findById(3L)).thenReturn(Optional.of(activity));
        when(venueRepository.findById(4L)).thenReturn(Optional.of(venue));
        when(ageGroupRepository.findById(5L)).thenReturn(Optional.of(ageGroup));
        when(languageRepository.findById(6L)).thenReturn(Optional.of(language));
        when(groupRepository.save(any(Group.class))).thenAnswer(inv -> inv.getArgument(0));

        GroupUpdateDTO dto = GroupUpdateDTO.builder()
                .titleDe("Kinder Tanz").titleEn("Kids Dance").titleUa("Дитячий танець")
                .capacity(12)
                .startDateTime(LocalDateTime.now().plusDays(1))
                .teacherId(2L).activityId(3L).venueId(4L).ageGroupId(5L).languageId(6L)
                .active(true)
                .build();

        Group updated = service().update(1L, dto);

        assertThat(updated.getTeacher()).isEqualTo(teacher);
        assertThat(updated.getActivity()).isEqualTo(activity);
        assertThat(updated.getVenue()).isEqualTo(venue);
        assertThat(updated.getAgeGroup()).isEqualTo(ageGroup);
        assertThat(updated.getLanguage()).isEqualTo(language);
        assertThat(updated.getCapacity()).isEqualTo(12);
    }

    // Same bug class as CourseService/WorkshopService this same session:
    // skip-if-null left a previously-set relation silently un-removable on
    // update. Authoritative-on-every-field fixes it here too.
    @Test
    void update_withNullIds_clearsPreviouslySetRelations() {
        Teacher oldTeacher = Teacher.builder().id(2L).build();
        Group existing = Group.builder().id(1L).titleDe("X").titleEn("X").titleUa("X").capacity(5)
                .teacher(oldTeacher)
                .activity(Activity.builder().id(3L).build())
                .venue(Venue.builder().id(4L).build())
                .ageGroup(AgeGroup.builder().id(5L).build())
                .language(Language.builder().id(6L).build())
                .build();

        when(groupRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(groupRepository.save(any(Group.class))).thenAnswer(inv -> inv.getArgument(0));

        GroupUpdateDTO dto = GroupUpdateDTO.builder()
                .titleDe("X").titleEn("X").titleUa("X")
                .capacity(5)
                .startDateTime(LocalDateTime.now().plusDays(1))
                .active(true)
                .build();

        Group updated = service().update(1L, dto);

        assertThat(updated.getTeacher()).isNull();
        assertThat(updated.getActivity()).isNull();
        assertThat(updated.getVenue()).isNull();
        assertThat(updated.getAgeGroup()).isNull();
        assertThat(updated.getLanguage()).isNull();
    }

    // LR-081 (LR-ADR-023) — same guard as createGroup, adapted to update()'s
    // reality: workshop can't be reassigned here, so the only way both end
    // up set is linking a course onto an already workshop-linked group.
    @Test
    void update_withCourseIdOnWorkshopLinkedGroup_throws() {
        Workshop workshop = Workshop.builder().id(1L).build();
        Group existing = Group.builder().id(1L).titleDe("X").titleEn("X").titleUa("X").capacity(5)
                .workshop(workshop)
                .build();
        Course course = Course.builder().id(9L).build();

        when(groupRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(courseRepository.findById(9L)).thenReturn(Optional.of(course));

        GroupUpdateDTO dto = GroupUpdateDTO.builder()
                .titleDe("X").titleEn("X").titleUa("X")
                .capacity(5)
                .startDateTime(LocalDateTime.now().plusDays(1))
                .courseId(9L)
                .active(true)
                .build();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service().update(1L, dto))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

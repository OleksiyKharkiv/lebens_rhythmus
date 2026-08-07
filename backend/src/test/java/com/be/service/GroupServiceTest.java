package com.be.service;

import com.be.domain.entity.*;
import com.be.domain.repository.*;
import com.be.web.dto.request.GroupCreateDTO;
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

    private GroupService service() {
        return new GroupService(groupRepository, workshopRepository, teacherRepository,
                activityRepository, venueRepository, ageGroupRepository);
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
}

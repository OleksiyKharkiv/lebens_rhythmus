package com.be.service;

import com.be.domain.entity.Course;
import com.be.domain.entity.User;
import com.be.domain.entity.Workshop;
import com.be.domain.repository.CourseRepository;
import com.be.domain.repository.UserRepository;
import com.be.domain.repository.WorkshopRepository;
import com.be.web.dto.request.WorkshopCreateDTO;
import com.be.web.mapper.WorkshopMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Regression coverage for the teacher-clearing bug found live in prod
 * 2026-08-09 (same root cause as CourseServiceTest's equivalent tests,
 * found first on Course, then confirmed by architect-reviewer to also
 * affect this class — the code Course.teacher's handling was modeled on).
 */
@ExtendWith(MockitoExtension.class)
class WorkshopServiceTest {

    @Mock
    private WorkshopRepository workshopRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private WorkshopMapper workshopMapper;

    private WorkshopService service() {
        return new WorkshopService(workshopRepository, userRepository, courseRepository, workshopMapper);
    }

    @Test
    void updateWorkshop_withNullTeacherId_clearsPreviouslySetTeacher() {
        User oldTeacher = User.builder().id(3L).firstName("Old").lastName("Teacher").build();
        Workshop existing = Workshop.builder()
                .id(1L)
                .workshopName("Theaterlabor")
                .teacher(oldTeacher)
                .build();

        when(workshopRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(workshopRepository.save(any(Workshop.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkshopCreateDTO dto = WorkshopCreateDTO.builder().teacherId(null).build();

        Workshop updated = service().updateWorkshop(1L, dto);

        assertThat(updated.getTeacher()).isNull();
        verifyNoInteractions(userRepository);
    }

    // LR-070 — courseId gets the same authoritative-clearing treatment
    // teacherId needed a live-prod fix for; proven from the start here.
    @Test
    void updateWorkshop_withNullCourseId_clearsPreviouslySetCourse() {
        Course oldCourse = Course.builder().id(4L)
                .titleDe("Theaterlabor").titleEn("Theater Lab").titleUa("Театральна лабораторія")
                .build();
        Workshop existing = Workshop.builder()
                .id(1L)
                .workshopName("Improv-Workshop")
                .course(oldCourse)
                .build();

        when(workshopRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(workshopRepository.save(any(Workshop.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkshopCreateDTO dto = WorkshopCreateDTO.builder().courseId(null).build();

        Workshop updated = service().updateWorkshop(1L, dto);

        assertThat(updated.getCourse()).isNull();
        verifyNoInteractions(courseRepository);
    }

    @Test
    void updateWorkshop_changesTeacher_withoutTouchingUnrelatedFields() {
        Workshop existing = Workshop.builder()
                .id(1L)
                .workshopName("Theaterlabor")
                .build();
        User newTeacher = User.builder().id(8L).firstName("Bea").lastName("Neu").build();

        when(workshopRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.findById(8L)).thenReturn(Optional.of(newTeacher));
        when(workshopRepository.save(any(Workshop.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkshopCreateDTO dto = WorkshopCreateDTO.builder().teacherId(8L).build();

        Workshop updated = service().updateWorkshop(1L, dto);

        assertThat(updated.getTeacher()).isEqualTo(newTeacher);
        assertThat(updated.getWorkshopName()).isEqualTo("Theaterlabor");
    }
}

package com.be.service;

import com.be.domain.entity.Course;
import com.be.domain.entity.Performance;
import com.be.domain.entity.Workshop;
import com.be.domain.repository.CourseRepository;
import com.be.domain.repository.PerformanceRepository;
import com.be.domain.repository.WorkshopRepository;
import com.be.web.dto.request.PerformanceRequestDTO;
import com.be.web.mapper.PerformanceMapper;
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
 * LR-071 (LR-ADR-021) — Performance.course added alongside the existing
 * Performance.workshop (both nullable, independent). Regression coverage
 * for the same "authoritative on every update" fix already needed live
 * for Course/Workshop teacher-clearing (2026-08-09/11) — applied here
 * from the start for both relations, not found live a third time.
 */
@ExtendWith(MockitoExtension.class)
class PerformanceServiceTest {

    @Mock
    private PerformanceRepository performanceRepository;
    @Mock
    private WorkshopRepository workshopRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private PerformanceMapper performanceMapper;

    private PerformanceService service() {
        return new PerformanceService(performanceRepository, workshopRepository, courseRepository, performanceMapper);
    }

    @Test
    void update_withNullWorkshopId_clearsPreviouslySetWorkshop() {
        Workshop oldWorkshop = Workshop.builder().id(2L).workshopName("Improv").build();
        Performance existing = Performance.builder()
                .id(1L)
                .title("Vorstellung")
                .workshop(oldWorkshop)
                .build();

        when(performanceRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(performanceRepository.save(any(Performance.class))).thenAnswer(inv -> inv.getArgument(0));

        PerformanceRequestDTO dto = PerformanceRequestDTO.builder().workshopId(null).build();

        Performance updated = service().update(1L, dto);

        assertThat(updated.getWorkshop()).isNull();
        verifyNoInteractions(workshopRepository);
    }

    @Test
    void update_withNullCourseId_clearsPreviouslySetCourse() {
        Course oldCourse = Course.builder().id(5L)
                .titleDe("Theaterlabor").titleEn("Theater Lab").titleUa("Театральна лабораторія")
                .build();
        Performance existing = Performance.builder()
                .id(1L)
                .title("Vorstellung")
                .course(oldCourse)
                .build();

        when(performanceRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(performanceRepository.save(any(Performance.class))).thenAnswer(inv -> inv.getArgument(0));

        PerformanceRequestDTO dto = PerformanceRequestDTO.builder().courseId(null).build();

        Performance updated = service().update(1L, dto);

        assertThat(updated.getCourse()).isNull();
        verifyNoInteractions(courseRepository);
    }

    @Test
    void create_withWorkshopAndCourseId_resolvesBothRelations() {
        Workshop workshop = Workshop.builder().id(2L).workshopName("Improv").build();
        Course course = Course.builder().id(5L)
                .titleDe("Theaterlabor").titleEn("Theater Lab").titleUa("Театральна лабораторія")
                .build();
        Performance mapped = Performance.builder().title("Vorstellung").build();

        when(performanceMapper.fromRequestDTO(any())).thenReturn(mapped);
        when(workshopRepository.findById(2L)).thenReturn(Optional.of(workshop));
        when(courseRepository.findById(5L)).thenReturn(Optional.of(course));
        when(performanceRepository.save(any(Performance.class))).thenAnswer(inv -> inv.getArgument(0));

        PerformanceRequestDTO dto = PerformanceRequestDTO.builder().workshopId(2L).courseId(5L).build();

        Performance created = service().create(dto);

        assertThat(created.getWorkshop()).isEqualTo(workshop);
        assertThat(created.getCourse()).isEqualTo(course);
    }
}

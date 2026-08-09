package com.be.service;

import com.be.domain.entity.AgeGroup;
import com.be.domain.entity.Course;
import com.be.domain.entity.User;
import com.be.domain.repository.AgeGroupRepository;
import com.be.domain.repository.CourseRepository;
import com.be.domain.repository.UserRepository;
import com.be.web.dto.request.CourseCreateDTO;
import com.be.web.mapper.CourseMapper;
import com.be.web.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * LR-069 (LR-ADR-023) — Course is purely descriptive, no schedule
 * fields; teacher is a temporary User FK (same shape as Workshop.teacher,
 * see LR-072), resolved the same way WorkshopService resolves teacherId.
 */
@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AgeGroupRepository ageGroupRepository;

    private CourseService service() {
        return new CourseService(courseRepository, userRepository, ageGroupRepository, new CourseMapper(new UserMapper()));
    }

    private CourseCreateDTO.CourseCreateDTOBuilder baseDto() {
        return CourseCreateDTO.builder()
                .titleDe("Theaterlabor").titleEn("Theater Lab").titleUa("Театральна лабораторія");
    }

    @Test
    void createCourse_withoutTeacherOrAgeGroup_persistsDescriptiveFieldsOnly() {
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        CourseCreateDTO dto = baseDto()
                .isOnline(true).isSynchronous(true).hasRecordings(false)
                .formatDisclaimerDe("Nur für Erwachsene, synchron, keine Aufzeichnung.")
                .build();

        Course created = service().createCourse(dto);

        assertThat(created.getTitleDe()).isEqualTo("Theaterlabor");
        assertThat(created.isOnline()).isTrue();
        assertThat(created.isSynchronous()).isTrue();
        assertThat(created.isHasRecordings()).isFalse();
        assertThat(created.getTeacher()).isNull();
        assertThat(created.getAgeGroup()).isNull();
    }

    @Test
    void createCourse_withTeacherId_resolvesRealUserEntity() {
        User teacher = User.builder().id(7L).firstName("Anna").lastName("Muster").build();
        when(userRepository.findById(7L)).thenReturn(Optional.of(teacher));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        CourseCreateDTO dto = baseDto().teacherId(7L).build();

        Course created = service().createCourse(dto);

        assertThat(created.getTeacher()).isEqualTo(teacher);
    }

    @Test
    void createCourse_withAgeGroupId_resolvesRealAgeGroupEntity() {
        AgeGroup ageGroup = AgeGroup.builder().id(3L).titleDe("Erwachsene").minAge(18).maxAge(99).build();
        when(ageGroupRepository.findById(3L)).thenReturn(Optional.of(ageGroup));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        CourseCreateDTO dto = baseDto().ageGroupId(3L).build();

        Course created = service().createCourse(dto);

        assertThat(created.getAgeGroup()).isEqualTo(ageGroup);
    }

    @Test
    void createCourse_withUnknownTeacherId_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        CourseCreateDTO dto = baseDto().teacherId(99L).build();

        assertThatThrownBy(() -> service().createCourse(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updateCourse_changesTeacher_withoutTouchingUnrelatedFields() {
        Course existing = Course.builder()
                .id(1L)
                .titleDe("Theaterlabor").titleEn("Theater Lab").titleUa("Театральна лабораторія")
                .isOnline(true).isSynchronous(true)
                .build();
        User newTeacher = User.builder().id(8L).firstName("Bea").lastName("Neu").build();

        when(courseRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.findById(8L)).thenReturn(Optional.of(newTeacher));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        CourseCreateDTO dto = CourseCreateDTO.builder().teacherId(8L).build();

        Course updated = service().updateCourse(1L, dto);

        assertThat(updated.getTeacher()).isEqualTo(newTeacher);
        assertThat(updated.getTitleDe()).isEqualTo("Theaterlabor");
        assertThat(updated.isOnline()).isTrue();
    }

    @Test
    void deleteCourse_unknownId_throws() {
        when(courseRepository.existsById(404L)).thenReturn(false);

        assertThatThrownBy(() -> service().deleteCourse(404L))
                .isInstanceOf(RuntimeException.class);
    }
}

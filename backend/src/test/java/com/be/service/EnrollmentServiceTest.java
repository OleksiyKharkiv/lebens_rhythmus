package com.be.service;

import com.be.domain.entity.*;
import com.be.domain.entity.enums.EnrollmentStatus;
import com.be.domain.exception.AlreadyEnrolledException;
import com.be.domain.exception.GroupFullException;
import com.be.domain.repository.*;
import com.be.web.dto.request.EnrollmentRequestDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * LR-084 — Enrollment extended from Workshop-only to also support Course,
 * and now links a paid enrollment to a system-created Order (the
 * "unfinished branch" found during the registration-architecture
 * roundtable, 2026-08-16). Also covers the atomic-capacity fix (was a
 * separate check-then-save race) and the free-vs-paid Order-creation split.
 */
@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private WorkshopRepository workshopRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private GroupRepository groupRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private NotificationService notificationService;

    private EnrollmentService service() {
        return new EnrollmentService(enrollmentRepository, userRepository, workshopRepository,
                courseRepository, groupRepository, orderRepository, notificationService);
    }

    private User user(long id) {
        return User.builder().id(id).email("test@example.com").build();
    }

    @Test
    void enroll_freeWorkshop_confirmsImmediately_noOrderCreated() {
        User user = user(1L);
        Workshop workshop = Workshop.builder().id(2L).price(null).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(workshopRepository.findById(2L)).thenReturn(Optional.of(workshop));
        when(enrollmentRepository.existsByUserIdAndWorkshopId(1L, 2L)).thenReturn(false);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        Enrollment result = service().enroll(2L, 1L, null);

        assertThat(result.getStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);
        assertThat(result.getOrder()).isNull();
        verify(orderRepository, never()).save(any());
    }

    @Test
    void enroll_paidWorkshop_pendingWithOrder() {
        User user = user(1L);
        Workshop workshop = Workshop.builder().id(2L).price(new BigDecimal("120.00")).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(workshopRepository.findById(2L)).thenReturn(Optional.of(workshop));
        when(enrollmentRepository.existsByUserIdAndWorkshopId(1L, 2L)).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(99L);
            return o;
        });
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        Enrollment result = service().enroll(2L, 1L, null);

        assertThat(result.getStatus()).isEqualTo(EnrollmentStatus.PENDING);
        assertThat(result.getOrder()).isNotNull();
        assertThat(result.getOrder().getAmount()).isEqualByComparingTo("120.00");
        assertThat(result.getOrder().getStatus()).isEqualTo("PENDING");
    }

    @Test
    void enroll_alreadyEnrolled_throws() {
        when(enrollmentRepository.existsByUserIdAndWorkshopId(1L, 2L)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
        when(workshopRepository.findById(2L)).thenReturn(Optional.of(Workshop.builder().id(2L).build()));

        assertThatThrownBy(() -> service().enroll(2L, 1L, null))
                .isInstanceOf(AlreadyEnrolledException.class)
                .hasMessageContaining("already enrolled");
    }

    // Was a separate getEnrolledCount()>=getCapacity() check + save(), not
    // atomic across two concurrent transactions — this test exercises the
    // new atomic decrementCapacityLeft path instead.
    @Test
    void enroll_groupFull_atomicDecrementReturnsZero_throws() {
        User user = user(1L);
        Workshop workshop = Workshop.builder().id(2L).build();
        Group group = Group.builder().id(3L).workshop(workshop).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(workshopRepository.findById(2L)).thenReturn(Optional.of(workshop));
        when(enrollmentRepository.existsByUserIdAndWorkshopId(1L, 2L)).thenReturn(false);
        when(groupRepository.findById(3L)).thenReturn(Optional.of(group));
        when(groupRepository.decrementCapacityLeft(3L)).thenReturn(0);

        EnrollmentRequestDTO request = EnrollmentRequestDTO.builder().groupId(3L).build();

        assertThatThrownBy(() -> service().enroll(2L, 1L, request))
                .isInstanceOf(GroupFullException.class)
                .hasMessageContaining("full");
    }

    @Test
    void enrollCourse_freeCourse_autoResolvesLinkedGroup_confirmsImmediately() {
        User user = user(1L);
        Course course = Course.builder().id(5L).price(null).build();
        Group linkedGroup = Group.builder().id(6L).course(course).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(courseRepository.findById(5L)).thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByUserIdAndCourseId(1L, 5L)).thenReturn(false);
        when(groupRepository.findByCourseId(5L)).thenReturn(List.of(linkedGroup));
        when(groupRepository.decrementCapacityLeft(6L)).thenReturn(1);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        Enrollment result = service().enrollCourse(5L, 1L, null);

        assertThat(result.getStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);
        assertThat(result.getCourse()).isEqualTo(course);
        assertThat(result.getGroup()).isEqualTo(linkedGroup);
        assertThat(result.getWorkshop()).isNull();
        verify(groupRepository).decrementCapacityLeft(6L);
    }

    @Test
    void enrollCourse_paidCourse_pendingWithOrder_linkedToCourse() {
        User user = user(1L);
        Course course = Course.builder().id(5L).price(new BigDecimal("165.00")).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(courseRepository.findById(5L)).thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByUserIdAndCourseId(1L, 5L)).thenReturn(false);
        when(groupRepository.findByCourseId(5L)).thenReturn(List.of());
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        Enrollment result = service().enrollCourse(5L, 1L, null);

        assertThat(result.getStatus()).isEqualTo(EnrollmentStatus.PENDING);
        assertThat(result.getOrder().getCourse()).isEqualTo(course);
        assertThat(result.getOrder().getWorkshop()).isNull();
    }

    @Test
    void cancelEnrollment_releasesGroupCapacity() {
        Group group = Group.builder().id(3L).build();
        Enrollment enrollment = Enrollment.builder()
                .id(10L)
                .user(user(1L))
                .group(group)
                .status(EnrollmentStatus.CONFIRMED)
                .build();

        when(enrollmentRepository.findById(10L)).thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        service().cancelEnrollment(10L, 1L, false);

        verify(groupRepository).incrementCapacityLeft(3L);
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
    }

    @Test
    void expire_releasesCapacityAndSetsExpired() {
        Group group = Group.builder().id(3L).build();
        Enrollment enrollment = Enrollment.builder()
                .id(11L)
                .user(user(1L))
                .group(group)
                .status(EnrollmentStatus.PENDING)
                .build();

        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));

        service().expire(enrollment);

        verify(groupRepository).incrementCapacityLeft(3L);
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.EXPIRED);
    }
}

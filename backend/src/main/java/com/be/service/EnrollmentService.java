package com.be.service;

import com.be.domain.entity.*;
import com.be.domain.entity.enums.EnrollmentStatus;
import com.be.domain.exception.AlreadyEnrolledException;
import com.be.domain.exception.GroupFullException;
import com.be.domain.repository.CourseRepository;
import com.be.domain.repository.EnrollmentRepository;
import com.be.domain.repository.GroupRepository;
import com.be.domain.repository.OrderRepository;
import com.be.domain.repository.UserRepository;
import com.be.domain.repository.WorkshopRepository;
import com.be.web.dto.request.EnrollmentRequestDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

// LR-084 — extended from Workshop-only to also support Course, and now
// creates an Order for paid enrollments (was: Enrollment only, no link to
// the payment side at all — the "unfinished branch" found during the
// registration-architecture roundtable, 2026-08-16).
@Service
@Transactional
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final WorkshopRepository workshopRepository;
    private final CourseRepository courseRepository;
    private final GroupRepository groupRepository;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService; // interface, use LogNotificationService in MVP

    public EnrollmentService(EnrollmentRepository enrollmentRepository,
                             UserRepository userRepository,
                             WorkshopRepository workshopRepository,
                             CourseRepository courseRepository,
                             GroupRepository groupRepository,
                             OrderRepository orderRepository,
                             NotificationService notificationService) {
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.workshopRepository = workshopRepository;
        this.courseRepository = courseRepository;
        this.groupRepository = groupRepository;
        this.orderRepository = orderRepository;
        this.notificationService = notificationService;
    }

    /**
     * Enroll current user into workshop (and optionally into a specific group).
     */
    public Enrollment enroll(Long workshopId, Long userId, EnrollmentRequestDTO request) {
        User user = findUser(userId);

        Workshop workshop = workshopRepository.findById(workshopId)
                .orElseThrow(() -> new RuntimeException("Workshop not found: " + workshopId));

        if (enrollmentRepository.existsByUserIdAndWorkshopId(userId, workshopId)) {
            throw new AlreadyEnrolledException("User already enrolled for this workshop");
        }

        Group group = null;
        if (request != null && request.getGroupId() != null) {
            group = resolveGroup(request.getGroupId(), g ->
                    g.getWorkshop() == null || !g.getWorkshop().getId().equals(workshopId),
                    "Group does not belong to the requested workshop");
        }

        Enrollment enrollment = Enrollment.builder()
                .user(user)
                .workshop(workshop)
                .group(group)
                .build();

        Order order = finalizeStatusAndOrder(enrollment, workshop.getPrice(), user, workshop, null);
        Enrollment saved = enrollmentRepository.save(enrollment);

        notify(workshop, null, group, user, saved.getStatus(), order);
        return saved;
    }

    /**
     * Enroll current user into a course. Unlike Workshop (which can have
     * several Groups, so the caller picks one), a Course has at most one
     * linked Group in the current MVP scope (LR-081) — resolved here, not
     * requested by the caller.
     */
    public Enrollment enrollCourse(Long courseId, Long userId, EnrollmentRequestDTO request) {
        User user = findUser(userId);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found: " + courseId));

        if (enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new AlreadyEnrolledException("User already enrolled for this course");
        }

        List<Group> linkedGroups = groupRepository.findByCourseId(courseId);
        Group group = linkedGroups.isEmpty() ? null : linkedGroups.get(0);
        if (group != null) {
            reserveCapacity(group.getId());
        }

        Enrollment enrollment = Enrollment.builder()
                .user(user)
                .course(course)
                .group(group)
                .build();

        Order order = finalizeStatusAndOrder(enrollment, course.getPrice(), user, null, course);
        Enrollment saved = enrollmentRepository.save(enrollment);

        notify(null, course, group, user, saved.getStatus(), order);
        return saved;
    }

    // Shared by enroll()/enrollCourse() — free (price null/0) confirms
    // immediately with no Order (nothing to charge); paid starts PENDING
    // with an Order created in the same transaction (was: Enrollment only,
    // no way to ever record that this registration needs paying).
    private Order finalizeStatusAndOrder(Enrollment enrollment, BigDecimal price, User user,
                                          Workshop workshop, Course course) {
        if (price == null || price.compareTo(BigDecimal.ZERO) == 0) {
            enrollment.setStatus(EnrollmentStatus.CONFIRMED);
            return null;
        }

        enrollment.setStatus(EnrollmentStatus.PENDING);
        // CODING_PROTOCOL.md §4b — status is never taken from a client DTO
        // here either; this Order is built entirely server-side.
        // architect-reviewer, 2026-08-16 — timestamp alone collides under
        // real concurrency (two paid enrollments in the same millisecond
        // both hit orderNumber's unique constraint); appended UUID fragment
        // makes collision practically impossible without losing the
        // human-readable timestamp prefix.
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .user(user)
                .workshop(workshop)
                .course(course)
                .amount(price)
                .currency("EUR")
                .quantity(1)
                .status("PENDING")
                .build();
        order = orderRepository.save(order);
        enrollment.setOrder(order);
        return order;
    }

    private String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private Group resolveGroup(Long groupId, java.util.function.Predicate<Group> mismatch, String mismatchMessage) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found: " + groupId));
        if (mismatch.test(group)) {
            throw new RuntimeException(mismatchMessage);
        }
        reserveCapacity(group.getId());
        return group;
    }

    // Atomic, WHERE-guarded decrement (GroupRepository.decrementCapacityLeft)
    // — closes the check-then-save race the roundtable flagged: the old
    // `group.getEnrolledCount() >= group.getCapacity()` check and the
    // eventual save() were two separate steps, so two concurrent requests
    // for the last open spot could both pass.
    private void reserveCapacity(Long groupId) {
        int updated = groupRepository.decrementCapacityLeft(groupId);
        if (updated == 0) {
            // architect-reviewer, 2026-08-16 (frontend round) — was a plain
            // RuntimeException, which GlobalExceptionHandler's generic
            // catch-all turns into a fixed English 500 message (LR-029) —
            // for Course specifically this is the everyday full-course
            // path, not a rare race, so it needs a clean, translatable
            // outcome, not a server-error-looking one.
            throw new GroupFullException("Group is full");
        }
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
    }

    private void notify(Workshop workshop, Course course, Group group, User user, EnrollmentStatus status, Order order) {
        try {
            String target = workshop != null ? workshop.getWorkshopName() : (course != null ? course.getTitleDe() : "?");
            String msg = String.format("New enrollment: user=%s (%d) for %s group=%s order=%s",
                    user.getEmail(), user.getId(), target,
                    group != null ? group.getId() : "n/a",
                    order != null ? order.getOrderNumber() : "n/a");
            notificationService.notifyEnrollment(workshop, course, group, user, status, msg);
        } catch (Exception ex) {
            // swallow notification errors — same reasoning as before this
            // change: a notification failure must never roll back a real
            // enrollment write.
        }
    }

    public List<Enrollment> getByUser(Long userId) {
        return enrollmentRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Enrollment> getByWorkshop(Long workshopId) {
        return enrollmentRepository.findByWorkshopId(workshopId);
    }

    public List<Enrollment> getByCourse(Long courseId) {
        return enrollmentRepository.findByCourseId(courseId);
    }

    public List<Enrollment> getByGroup(Long groupId) {
        return enrollmentRepository.findByGroupId(groupId);
    }

    public void cancelEnrollment(Long enrollmentId, Long actorUserId, boolean isAdmin) {
        Enrollment e = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found: " + enrollmentId));

        // actor may cancel only own enrollment or admin/owner
        if (!isAdmin && !e.getUser().getId().equals(actorUserId)) {
            throw new RuntimeException("Forbidden: cannot cancel other user's enrollment");
        }

        // architect-reviewer, 2026-08-16 — without this, cancelling an
        // already-EXPIRED enrollment silently flips it to CANCELLED with no
        // error, muddying the CANCELLED-vs-EXPIRED reporting split this
        // class otherwise keeps clean (see expire()'s own comment).
        if (e.getStatus() == EnrollmentStatus.CANCELLED || e.getStatus() == EnrollmentStatus.EXPIRED) {
            throw new RuntimeException("Enrollment is already " + e.getStatus());
        }

        releaseIfHeldCapacity(e);
        e.setStatus(EnrollmentStatus.CANCELLED);
        enrollmentRepository.save(e);

        // notify
        try {
            notificationService.notifyEnrollmentCancelled(e.getWorkshop(), e.getCourse(), e.getGroup(), e.getUser(),
                    e.getStatus(), "Enrollment cancelled: " + e.getId());
        } catch (Exception ignored) {
        }
    }

    // LR-084 — 7-day TTL cleanup (EnrollmentCleanupService) calls this for
    // every stale PENDING enrollment. Separate from cancelEnrollment (an
    // EXPIRED enrollment was never actively cancelled by anyone — kept as
    // its own status so the two are never conflated in reporting).
    public void expire(Enrollment e) {
        releaseIfHeldCapacity(e);
        e.setStatus(EnrollmentStatus.EXPIRED);
        enrollmentRepository.save(e);
    }

    private void releaseIfHeldCapacity(Enrollment e) {
        if (e.getGroup() != null && (e.getStatus() == EnrollmentStatus.PENDING || e.getStatus() == EnrollmentStatus.CONFIRMED)) {
            groupRepository.incrementCapacityLeft(e.getGroup().getId());
        }
    }
}

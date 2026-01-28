package com.be.service;

import com.be.domain.entity.*;
import com.be.domain.entity.enums.EnrollmentStatus;
import com.be.domain.repository.EnrollmentRepository;
import com.be.domain.repository.GroupRepository;
import com.be.domain.repository.UserRepository;
import com.be.domain.repository.WorkshopRepository;
import com.be.web.dto.request.EnrollmentRequestDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final WorkshopRepository workshopRepository;
    private final GroupRepository groupRepository;
    private final NotificationService notificationService; // interface, use LogNotificationService in MVP

    public EnrollmentService(EnrollmentRepository enrollmentRepository,
                             UserRepository userRepository,
                             WorkshopRepository workshopRepository,
                             GroupRepository groupRepository,
                             NotificationService notificationService) {
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.workshopRepository = workshopRepository;
        this.groupRepository = groupRepository;
        this.notificationService = notificationService;
    }

    /**
     * Enroll current user into workshop (and optionally into a specific group).
     */
    public Enrollment enroll(Long workshopId, Long userId, EnrollmentRequestDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        Workshop workshop = workshopRepository.findById(workshopId)
                .orElseThrow(() -> new RuntimeException("Workshop not found: " + workshopId));

        if (enrollmentRepository.existsByUserIdAndWorkshopId(userId, workshopId)) {
            throw new RuntimeException("User already enrolled for this workshop");
        }

        Group group = null;
        if (request != null && request.getGroupId() != null) {
            group = groupRepository.findById(request.getGroupId())
                    .orElseThrow(() -> new RuntimeException("Group not found: " + request.getGroupId()));

            // group must belong to the workshop
            if (group.getWorkshop() == null || !group.getWorkshop().getId().equals(workshopId)) {
                throw new RuntimeException("Group does not belong to the requested workshop");
            }

            // check capacity
            if (group.getEnrolledCount() >= group.getCapacity()) {
                throw new RuntimeException("Group is full");
            }
        }

        Enrollment enrollment = Enrollment.builder()
                .user(user)
                .workshop(workshop)
                .group(group)
                .build();

        // determine status: free workshop -> CONFIRMED, else PENDING
        BigDecimal price = workshop.getPrice();
        if (price == null || price.compareTo(BigDecimal.ZERO) == 0) {
            enrollment.setStatus(EnrollmentStatus.CONFIRMED);
        } else {
            enrollment.setStatus(EnrollmentStatus.PENDING);
        }

        Enrollment saved = enrollmentRepository.save(enrollment);

        // Notify teacher and business owner (MVP: logs)
        try {
            String msg = String.format("New enrollment: user=%s (%d) for workshop=%s (%d) group=%s",
                    user.getEmail(), user.getId(), workshop.getWorkshopName(), workshop.getId(),
                    group != null ? group.getId() : "n/a");
            notificationService.notifyWorkshopEnrollment(workshop, group, user, saved.getStatus(), msg);
        } catch (Exception ex) {
            // swallow notification errors for now but log (NotificationService implementation should log)
        }

        return saved;
    }

    public List<Enrollment> getByUser(Long userId) {
        return enrollmentRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Enrollment> getByWorkshop(Long workshopId) {
        return enrollmentRepository.findByWorkshopId(workshopId);
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

        e.setStatus(EnrollmentStatus.CANCELLED);
        enrollmentRepository.save(e);

        // notify
        try {
            notificationService.notifyEnrollmentCancelled(e.getWorkshop(), e.getGroup(), e.getUser(), e.getStatus(),
                    "Enrollment cancelled: " + e.getId());
        } catch (Exception ignored) {
        }
    }
}
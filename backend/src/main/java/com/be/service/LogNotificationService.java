package com.be.service;

import com.be.domain.entity.Enrollment.EnrollmentStatus;
import com.be.domain.entity.Group;
import com.be.domain.entity.User;
import com.be.domain.entity.Workshop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LogNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(LogNotificationService.class);

    @Override
    public void notifyWorkshopEnrollment(Workshop workshop, Group group, User user, EnrollmentStatus status, String message) {
        log.info("NOTIFY ENROLLMENT: status={} user={} workshop={} group={} msg={}",
                status, user.getEmail(), workshop != null ? workshop.getWorkshopName() : null,
                group != null ? group.getId() : null, message);
    }

    @Override
    public void notifyEnrollmentCancelled(Workshop workshop, Group group, User user, EnrollmentStatus status, String message) {
        log.info("NOTIFY CANCELLATION: status={} user={} workshop={} group={} msg={}",
                status, user.getEmail(), workshop != null ? workshop.getWorkshopName() : null,
                group != null ? group.getId() : null, message);
    }
}

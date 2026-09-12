package com.be.service;

import com.be.domain.entity.Course;
import com.be.domain.entity.Group;
import com.be.domain.entity.User;
import com.be.domain.entity.Workshop;
import com.be.domain.entity.enums.EnrollmentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LogNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(LogNotificationService.class);

    // Security checklist Tier 0.5/4.3 (2026-09-10) — this is the sole
    // production NotificationService impl (@Service, no @Profile guard),
    // fired on every real enrollment/cancellation. It used to log
    // user.getEmail() at INFO — not suppressed by the com.be=INFO prod
    // default (LR-034), so it wrote a real user's email to the on-disk
    // app log unconditionally. User id is enough to correlate an event to
    // a user for ops purposes; the email itself has no operational value
    // here and is unnecessary PII exposure in a log with broader
    // retention/access than the primary DB.
    @Override
    public void notifyEnrollment(Workshop workshop, Course course, Group group, User user, EnrollmentStatus status, String message) {
        log.info("NOTIFY ENROLLMENT: status={} userId={} workshop={} course={} group={} msg={}",
                status, user.getId(), workshop != null ? workshop.getWorkshopName() : null,
                course != null ? course.getTitleDe() : null,
                group != null ? group.getId() : null, message);
    }

    @Override
    public void notifyEnrollmentCancelled(Workshop workshop, Course course, Group group, User user, EnrollmentStatus status, String message) {
        log.info("NOTIFY CANCELLATION: status={} userId={} workshop={} course={} group={} msg={}",
                status, user.getId(), workshop != null ? workshop.getWorkshopName() : null,
                course != null ? course.getTitleDe() : null,
                group != null ? group.getId() : null, message);
    }
}
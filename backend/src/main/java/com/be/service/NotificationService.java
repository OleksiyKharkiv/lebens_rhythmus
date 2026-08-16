package com.be.service;

import com.be.domain.entity.Course;
import com.be.domain.entity.enums.EnrollmentStatus;
import com.be.domain.entity.Group;
import com.be.domain.entity.User;
import com.be.domain.entity.Workshop;

// LR-084 — workshop/course both nullable, exactly one set (mirrors
// Enrollment.workshop/course itself) — renamed from notifyWorkshopEnrollment
// now that it also covers Course, rather than adding a parallel
// notifyCourseEnrollment pair.
public interface NotificationService {
    void notifyEnrollment(Workshop workshop, Course course, Group group, User user, EnrollmentStatus status, String message);

    void notifyEnrollmentCancelled(Workshop workshop, Course course, Group group, User user, EnrollmentStatus status, String message);
}
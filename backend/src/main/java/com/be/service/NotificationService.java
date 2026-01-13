package com.be.service;

import com.be.domain.entity.enums.EnrollmentStatus;
import com.be.domain.entity.Group;
import com.be.domain.entity.User;
import com.be.domain.entity.Workshop;

public interface NotificationService {
    void notifyWorkshopEnrollment(Workshop workshop, Group group, User user, EnrollmentStatus status, String message);
    void notifyEnrollmentCancelled(Workshop workshop, Group group, User user, EnrollmentStatus status, String message);
}

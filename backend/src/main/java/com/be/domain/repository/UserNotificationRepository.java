package com.be.domain.repository;

import com.be.domain.entity.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {
    java.util.List<UserNotification> findByUserId(Long userId);
}

package com.be.service;

import com.be.domain.entity.UserNotification;
import com.be.domain.entity.User;
import com.be.domain.entity.Notification;
import com.be.domain.repository.UserNotificationRepository;
import com.be.domain.repository.UserRepository;
import com.be.domain.repository.NotificationRepository;
import com.be.web.dto.request.UserNotificationRequestDTO;
import com.be.web.mapper.UserNotificationMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class UserNotificationService {

    private final UserNotificationRepository userNotificationRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final UserNotificationMapper userNotificationMapper;

    public UserNotificationService(UserNotificationRepository userNotificationRepository,
                                   UserRepository userRepository,
                                   NotificationRepository notificationRepository,
                                   UserNotificationMapper userNotificationMapper) {
        this.userNotificationRepository = userNotificationRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.userNotificationMapper = userNotificationMapper;
    }

    @Transactional(readOnly = true)
    public List<UserNotification> getByUserId(Long userId) {
        return userNotificationRepository.findByUserId(userId);
    }

    @Transactional
    public UserNotification create(UserNotificationRequestDTO dto) {
        UserNotification un = userNotificationMapper.fromRequestDTO(dto);

        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        un.setUser(user);

        Notification notification = notificationRepository.findById(dto.getNotificationId())
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        un.setNotification(notification);

        return userNotificationRepository.save(un);
    }

    @Transactional
    public UserNotification markAsRead(Long id) {
        UserNotification un = userNotificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("UserNotification not found"));
        un.setRead(true);
        un.setReadAt(LocalDateTime.now());
        return userNotificationRepository.save(un);
    }
}
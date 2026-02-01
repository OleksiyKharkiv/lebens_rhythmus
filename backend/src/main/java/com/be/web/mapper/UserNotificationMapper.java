package com.be.web.mapper;

import com.be.domain.entity.UserNotification;
import com.be.web.dto.request.UserNotificationRequestDTO;
import com.be.web.dto.response.UserNotificationResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class UserNotificationMapper {

    public UserNotificationResponseDTO toResponseDTO(UserNotification userNotification) {
        if (userNotification == null) return null;
        return UserNotificationResponseDTO.builder()
                .id(userNotification.getId())
                .userId(userNotification.getUser() != null ? userNotification.getUser().getId() : null)
                .username(userNotification.getUser() != null ? (userNotification.getUser().getFirstName() + " " + userNotification.getUser().getLastName()) : null)
                .notificationId(userNotification.getNotification() != null ? userNotification.getNotification().getId() : null)
                .notificationTitle(userNotification.getNotification() != null ? userNotification.getNotification().getTitle() : null)
                .read(userNotification.isRead())
                .readAt(userNotification.getReadAt())
                .build();
    }

    public UserNotification fromRequestDTO(UserNotificationRequestDTO dto) {
        if (dto == null) return null;
        return UserNotification.builder()
                .read(dto.isRead())
                .build();
    }
}

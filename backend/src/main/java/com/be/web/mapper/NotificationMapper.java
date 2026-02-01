package com.be.web.mapper;

import com.be.domain.entity.Notification;
import com.be.web.dto.request.NotificationRequestDTO;
import com.be.web.dto.response.NotificationResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationResponseDTO toResponseDTO(Notification notification) {
        if (notification == null) return null;
        return NotificationResponseDTO.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    public Notification fromRequestDTO(NotificationRequestDTO dto) {
        if (dto == null) return null;
        return Notification.builder()
                .title(dto.getTitle())
                .message(dto.getMessage())
                .type(dto.getType())
                .build();
    }
}

package com.be.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserNotificationResponseDTO {
    private Long id;
    private Long userId;
    private String username;
    private Long notificationId;
    private String notificationTitle;
    private boolean read;
    private LocalDateTime readAt;
}

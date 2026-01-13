package com.be.web.dto.response;

import com.be.domain.entity.enums.EnrollmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentAdminDTO {
    private Long id;
    private Long workshopId;
    private String workshopTitle;
    private Long groupId;
    private String groupTitle;
    private EnrollmentStatus status;
    private LocalDateTime createdAt;

    // user info
    private UserBasicDTO user;
}
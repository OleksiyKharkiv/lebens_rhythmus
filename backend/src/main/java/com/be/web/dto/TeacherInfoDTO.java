package com.be.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherInfoDTO {
    private Long id;
    private String title;
    private String bio;
    private Boolean approved;
    private Long userId;
}
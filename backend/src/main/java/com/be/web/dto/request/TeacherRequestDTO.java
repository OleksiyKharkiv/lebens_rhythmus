package com.be.web.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherRequestDTO {
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String title;
    private boolean approved;
    private String bioDe;
    private String bioEn;
    private String bioUa;
    private boolean active;
}

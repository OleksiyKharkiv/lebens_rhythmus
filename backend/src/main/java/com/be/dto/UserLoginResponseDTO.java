package com.be.dto;

import com.be.domain.entity.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginResponseDTO {
    private String token;
    private String tokenType = "Bearer";
    private Long expireIn; // in seconds
    //    Basic user info
    private Long userId;
    private String email;
    private String firstName;
    private String lastName;
    private Role role;

//    Profile connections
    private List<ParticipantInfoDTO> participants; //All participants of this user
    private List<TeacherInfoDTO>teachers;

}

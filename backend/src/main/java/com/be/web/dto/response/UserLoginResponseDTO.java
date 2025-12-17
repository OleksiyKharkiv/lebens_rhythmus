package com.be.web.dto.response;

import com.be.domain.entity.enums.Role;
import com.be.web.dto.ParticipantInfoDTO;
import com.be.web.dto.TeacherInfoDTO;
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
    @Builder.Default
    private String tokenType = "Bearer";
    private Long expiresIn; // in seconds
    //    Basic user info
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private Role role;

//    Profile connections
    private List<ParticipantInfoDTO> participants; //All participants of this user
    private List<TeacherInfoDTO>teachers;

}

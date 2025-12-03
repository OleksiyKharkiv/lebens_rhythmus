package com.be.web.mapper;

import com.be.domain.entity.User;
import com.be.web.dto.ParticipantInfoDTO;
import com.be.web.dto.TeacherInfoDTO;
import com.be.web.dto.request.UserRegistrationDTO;
import com.be.web.dto.request.UserUpdateDTO;
import com.be.web.dto.response.UserLoginResponseDTO;
import com.be.web.dto.response.UserProfileDTO;
import com.be.web.dto.response.UserBasicDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    public User toEntity(UserRegistrationDTO dto) {
        return User.builder().email(dto.getEmail()).password(dto.getPassword()).firstName(dto.getFirstName()).lastName(dto.getLastName()).phone(dto.getPhone()).birthDate(dto.getBirthDate()).role(dto.getRole()).address(dto.getAddress()).city(dto.getCity()).zipCode(dto.getZipCode()).country(dto.getCountry()).acceptedTerms(dto.getAcceptedTerms()).privacyPolicyAccepted(dto.getPrivacyPolicyAccepted()).build();
    }

    public void updateEntityFromDTO(UserUpdateDTO dto, User user) {
        if (dto.getFirstName() != null) user.setFirstName(dto.getFirstName());
        if (dto.getLastName() != null) user.setLastName(dto.getLastName());
        if (dto.getPhone() != null) user.setPhone(dto.getPhone());
        if (dto.getBirthDate() != null) user.setBirthDate(dto.getBirthDate());
        if (dto.getAddress() != null) user.setAddress(dto.getAddress());
        if (dto.getCity() != null) user.setCity(dto.getCity());
        if (dto.getZipCode() != null) user.setZipCode(dto.getZipCode());
        if (dto.getCountry() != null) user.setCountry(dto.getCountry());
        if (dto.getTitle() != null) user.setTitle(dto.getTitle());
        if (dto.getBio() != null) user.setBio(dto.getBio());
    }

    // ========== ENTITY → RESPONSE DTO ==========
    public UserLoginResponseDTO toLoginResponseDTO(User user, String token, Long expiresIn, List<ParticipantInfoDTO> participants, List<TeacherInfoDTO> teachers) {
        return UserLoginResponseDTO.builder().token(token).tokenType("Bearer").expiresIn(expiresIn).id(user.getId()).email(user.getEmail()).firstName(user.getFirstName()).lastName(user.getLastName()).role(user.getRole()).participants(participants).teachers(teachers).build();
    }

    public UserProfileDTO toProfileDTO(User user) {
        return UserProfileDTO.builder().id(user.getId()).email(user.getEmail()).firstName(user.getFirstName()).lastName(user.getLastName()).phone(user.getPhone()).birthDate(user.getBirthDate()).role(user.getRole()).address(user.getAddress()).city(user.getCity()).zipCode(user.getZipCode()).country(user.getCountry()).title(user.getTitle()).bio(user.getBio()).enabled(user.getEnabled()).emailVerified(user.getEmailVerified()).termsAcceptedAt(user.getTermsAcceptedAt()).privacyPolicyAcceptedAt(user.getPrivacyPolicyAcceptedAt()).createdAt(user.getCreatedAt()).updatedAt(user.getUpdatedAt()).build();
    }

    public UserBasicDTO toBasicDTO(User user) {
        return UserBasicDTO.builder().id(user.getId()).email(user.getEmail()).firstName(user.getFirstName()).lastName(user.getLastName()).role(user.getRole()).enabled(user.getEnabled()).build();
    }

    public List<UserBasicDTO> toBasicDTOList(List<User> users) {
        return users.stream().map(this::toBasicDTO).collect(Collectors.toList());
    }
}

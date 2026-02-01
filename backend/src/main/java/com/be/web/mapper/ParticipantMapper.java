package com.be.web.mapper;

import com.be.domain.entity.Participant;
import com.be.web.dto.request.ParticipantRequestDTO;
import com.be.web.dto.response.ParticipantResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class ParticipantMapper {

    public ParticipantResponseDTO toResponseDTO(Participant participant) {
        if (participant == null) return null;
        return ParticipantResponseDTO.builder()
                .id(participant.getId())
                .firstName(participant.getFirstName())
                .lastName(participant.getLastName())
                .email(participant.getEmail())
                .phone(participant.getPhone())
                .birthDate(participant.getBirthDate())
                .groupId(participant.getGroup() != null ? participant.getGroup().getId() : null)
                .active(participant.isActive())
                .build();
    }

    public Participant fromRequestDTO(ParticipantRequestDTO dto) {
        if (dto == null) return null;
        return Participant.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .birthDate(dto.getBirthDate())
                .active(dto.isActive())
                .build();
    }
}

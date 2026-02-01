package com.be.web.mapper;

import com.be.domain.entity.AgeGroup;
import com.be.web.dto.request.AgeGroupRequestDTO;
import com.be.web.dto.response.AgeGroupResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class AgeGroupMapper {

    public AgeGroupResponseDTO toResponseDTO(AgeGroup ageGroup) {
        if (ageGroup == null) return null;
        return AgeGroupResponseDTO.builder()
                .id(ageGroup.getId())
                .titleDe(ageGroup.getTitleDe())
                .titleEn(ageGroup.getTitleEn())
                .titleUa(ageGroup.getTitleUa())
                .minAge(ageGroup.getMinAge())
                .maxAge(ageGroup.getMaxAge())
                .build();
    }

    public AgeGroup fromRequestDTO(AgeGroupRequestDTO dto) {
        if (dto == null) return null;
        return AgeGroup.builder()
                .titleDe(dto.getTitleDe())
                .titleEn(dto.getTitleEn())
                .titleUa(dto.getTitleUa())
                .minAge(dto.getMinAge())
                .maxAge(dto.getMaxAge())
                .build();
    }
}

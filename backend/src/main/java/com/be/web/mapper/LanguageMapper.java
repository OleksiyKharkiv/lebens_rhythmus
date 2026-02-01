package com.be.web.mapper;

import com.be.domain.entity.Language;
import com.be.web.dto.request.LanguageRequestDTO;
import com.be.web.dto.response.LanguageResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class LanguageMapper {

    public LanguageResponseDTO toResponseDTO(Language language) {
        if (language == null) return null;
        return LanguageResponseDTO.builder()
                .id(language.getId())
                .nameDe(language.getNameDe())
                .nameEn(language.getNameEn())
                .nameUa(language.getNameUa())
                .code(language.getCode())
                .active(language.isActive())
                .build();
    }

    public Language fromRequestDTO(LanguageRequestDTO dto) {
        if (dto == null) return null;
        return Language.builder()
                .nameDe(dto.getNameDe())
                .nameEn(dto.getNameEn())
                .nameUa(dto.getNameUa())
                .code(dto.getCode())
                .active(dto.isActive())
                .build();
    }
}

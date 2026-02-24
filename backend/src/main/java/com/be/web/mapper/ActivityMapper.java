package com.be.web.mapper;

import com.be.domain.entity.Activity;
import com.be.web.dto.request.ActivityRequestDTO;
import com.be.web.dto.response.ActivityResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class ActivityMapper {

    public ActivityResponseDTO toResponseDTO(Activity activity) {
        if (activity == null) return null;
        return ActivityResponseDTO.builder()
                .id(activity.getId())
                .titleDe(activity.getTitleDe())
                .titleEn(activity.getTitleEn())
                .titleUa(activity.getTitleUa())
                .descriptionDe(activity.getDescriptionDe())
                .descriptionEn(activity.getDescriptionEn())
                .descriptionUa(activity.getDescriptionUa())
                .price(activity.getPrice())
                .durationMinutes(activity.getDurationMinutes())
                .active(activity.isActive())
                .build();
    }

    public Activity fromRequestDTO(ActivityRequestDTO dto) {
        if (dto == null) return null;
        return Activity.builder()
                .titleDe(dto.getTitleDe())
                .titleEn(dto.getTitleEn())
                .titleUa(dto.getTitleUa())
                .descriptionDe(dto.getDescriptionDe())
                .descriptionEn(dto.getDescriptionEn())
                .descriptionUa(dto.getDescriptionUa())
                .price(dto.getPrice())
                .durationMinutes(dto.getDurationMinutes())
                .active(dto.isActive())
                .build();
    }
}
package com.be.web.mapper;

import com.be.domain.entity.Performance;
import com.be.web.dto.request.PerformanceRequestDTO;
import com.be.web.dto.response.PerformanceResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class PerformanceMapper {

    public PerformanceResponseDTO toResponseDTO(Performance performance) {
        if (performance == null) return null;
        return PerformanceResponseDTO.builder()
                .id(performance.getId())
                .workshopId(performance.getWorkshop() != null ? performance.getWorkshop().getId() : null)
                .workshopTitle(performance.getWorkshop() != null ? performance.getWorkshop().getWorkshopName() : null)
                .title(performance.getTitle())
                .description(performance.getDescription())
                .performanceDate(performance.getPerformanceDate())
                .venue(performance.getVenue())
                .maxAttendees(performance.getMaxAttendees())
                .status(performance.getStatus() != null ? performance.getStatus().name() : null)
                .createdAt(performance.getCreatedAt())
                .updatedAt(performance.getUpdatedAt())
                .build();
    }

    public Performance fromRequestDTO(PerformanceRequestDTO dto) {
        if (dto == null) return null;
        return Performance.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .performanceDate(dto.getPerformanceDate())
                .venue(dto.getVenue())
                .maxAttendees(dto.getMaxAttendees())
                .build();
    }
}

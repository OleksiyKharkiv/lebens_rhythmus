package com.be.web.mapper;

import com.be.domain.entity.Event;
import com.be.web.dto.request.EventRequestDTO;
import com.be.web.dto.response.EventResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class EventMapper {

    public EventResponseDTO toResponseDTO(Event event) {
        if (event == null) return null;
        return EventResponseDTO.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .startDateTime(event.getStartDateTime())
                .endDateTime(event.getEndDateTime())
                .venueId(event.getVenue() != null ? event.getVenue().getId() : null)
                .venueName(event.getVenue() != null ? event.getVenue().getName() : null)
                .workshopId(event.getWorkshop() != null ? event.getWorkshop().getId() : null)
                .workshopTitle(event.getWorkshop() != null ? event.getWorkshop().getWorkshopName() : null)
                .contractId(event.getContract() != null ? event.getContract().getId() : null)
                .contractNumber(event.getContract() != null ? event.getContract().getContractNumber() : null)
                .price(event.getPrice())
                .currency(event.getCurrency())
                .capacity(event.getCapacity())
                .status(event.getStatus())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }

    public Event fromRequestDTO(EventRequestDTO dto) {
        if (dto == null) return null;
        return Event.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .startDateTime(dto.getStartDateTime())
                .endDateTime(dto.getEndDateTime())
                .price(dto.getPrice())
                .currency(dto.getCurrency())
                .capacity(dto.getCapacity())
                .status(dto.getStatus())
                .build();
    }
}

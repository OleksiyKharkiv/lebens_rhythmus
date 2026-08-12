package com.be.web.mapper;

import com.be.domain.entity.Session;
import com.be.domain.entity.Venue;
import com.be.web.dto.response.SessionDTO;
import org.springframework.stereotype.Component;

@Component
public class SessionMapper {

    public SessionDTO toDto(Session s) {
        return SessionDTO.builder()
                .id(s.getId())
                .startDateTime(s.getStartDateTime())
                .endDateTime(s.getEndDateTime())
                .venueId(s.getVenue() != null ? s.getVenue().getId() : null)
                .venueName(s.getVenue() != null ? formatVenueName(s.getVenue()) : null)
                .build();
    }

    // Same "name — room" convention as WorkshopMapper.formatVenueName (LR-015).
    private String formatVenueName(Venue v) {
        return (v.getRoom() == null || v.getRoom().isBlank())
                ? v.getName()
                : v.getName() + " — " + v.getRoom();
    }
}

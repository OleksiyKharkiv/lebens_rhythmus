package com.be.web.mapper;

import com.be.domain.entity.Feedback;
import com.be.web.dto.request.FeedbackRequestDTO;
import com.be.web.dto.response.FeedbackResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class FeedbackMapper {

    public FeedbackResponseDTO toResponseDTO(Feedback feedback) {
        if (feedback == null) return null;
        String username = null;
        if (feedback.getUser() != null) {
            username = feedback.getUser().getFirstName() + " " + feedback.getUser().getLastName();
        }
        return FeedbackResponseDTO.builder()
                .id(feedback.getId())
                .userId(feedback.getUser() != null ? feedback.getUser().getId() : null)
                .username(username)
                .content(feedback.getContent())
                .rating(feedback.getRating())
                .createdAt(feedback.getCreatedAt())
                .build();
    }

    public Feedback fromRequestDTO(FeedbackRequestDTO dto) {
        if (dto == null) return null;
        return Feedback.builder()
                .content(dto.getContent())
                .rating(dto.getRating())
                .build();
    }
}

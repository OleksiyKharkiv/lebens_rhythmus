package com.be.web.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionWriteDTO {
    @NotNull
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private Long venueId;
}

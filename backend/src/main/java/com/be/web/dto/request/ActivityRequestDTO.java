package com.be.web.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityRequestDTO {
    private String titleDe;
    private String titleEn;
    private String titleUa;
    private String descriptionDe;
    private String descriptionEn;
    private String descriptionUa;
    private BigDecimal price;
    private int durationMinutes;
    private boolean active;
}

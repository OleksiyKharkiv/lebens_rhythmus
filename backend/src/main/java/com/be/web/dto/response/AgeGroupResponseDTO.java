package com.be.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgeGroupResponseDTO {
    private Long id;
    private String titleDe;
    private String titleEn;
    private String titleUa;
    private int minAge;
    private int maxAge;
}

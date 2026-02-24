package com.be.web.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgeGroupRequestDTO {
    private String titleDe;
    private String titleEn;
    private String titleUa;
    private int minAge;
    private int maxAge;
}
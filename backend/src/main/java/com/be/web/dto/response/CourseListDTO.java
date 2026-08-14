package com.be.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseListDTO {
    private Long id;
    private String titleDe;
    private String titleEn;
    private String titleUa;
    private String shortDescriptionDe;
    private UserBasicDTO teacher;
    private Boolean isOnline;
    private Boolean isSynchronous;
    private Boolean hasRecordings;
    private BigDecimal price;
    private String status;
}

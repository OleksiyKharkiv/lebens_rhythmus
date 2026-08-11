package com.be.web.dto.response;

import com.be.web.dto.TeacherInfoDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkshopListDTO {
    private Long id;
    private String title;
    private String shortDescription;
    private TeacherInfoDTO teacher;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal price;
    private String status;
}
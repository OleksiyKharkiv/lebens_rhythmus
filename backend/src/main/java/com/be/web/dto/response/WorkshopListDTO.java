package com.be.web.dto.response;

import com.be.web.dto.TeacherInfoDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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
    // Registration button on the public list page (2026-08-16) needs to
    // know how many active Groups (sessions) exist and, when there's
    // exactly one, its id/capacity to enroll directly — same reasoning
    // as Course's single-Group MVP auto-resolve. With 2+ groups the
    // frontend can't pick one for the user, so it falls back to a
    // "choose a date" link instead of a direct enroll.
    private List<GroupDTO> groups;
}
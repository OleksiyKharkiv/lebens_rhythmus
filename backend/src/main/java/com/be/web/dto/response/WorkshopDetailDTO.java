package com.be.web.dto.response;

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
public class WorkshopDetailDTO {
    private Long id;
    private String title;
    private String description;
    private UserBasicDTO teacher;
    private LocalDate startDate;
    private LocalDate endDate;
    // venueName/venueId removed (LR-015) — venue is per-Group now, see
    // GroupDTO.venueName within `groups` below, not a single workshop-wide value.
    private BigDecimal price;
    private String status;
    private List<GroupDTO> groups;
    private List<WorkshopFileDTO> files;
    private Integer totalEnrollments;
}
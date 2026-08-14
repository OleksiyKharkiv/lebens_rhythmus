package com.be.web.dto.request;

import com.be.domain.entity.RecurrenceDay;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// Closes the last @RequestBody-raw-entity gap on GroupController — createGroup
// was already fixed this way (LR-030, see GroupCreateDTO); update() bound
// `Group` directly, so a crafted body could set capacityLeft, or reference an
// existing Enrollment's id inside enrollments (CascadeType.ALL,
// orphanRemoval=true) to re-parent it onto this group. Explicit fields only,
// ids resolved server-side in GroupService.update.
//
// No workshopId — group -> workshop reassignment was never supported on
// update (GroupService.update's own prior comment: "a bigger decision than a
// same-shape field copy"), unaffected by this fix.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupUpdateDTO {
    private String titleDe;
    private String titleEn;
    private String titleUa;
    private Integer capacity;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private Long teacherId;
    private Long activityId;
    private Long venueId;
    private Long ageGroupId;
    private Long languageId;
    private boolean active;

    private Long courseId;
    private List<RecurrenceDay> recurrenceDays;
    private LocalDate recurrenceStartDate;
    private LocalDate recurrenceEndDate;
}

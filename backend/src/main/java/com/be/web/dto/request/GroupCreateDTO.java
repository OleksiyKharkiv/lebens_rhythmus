package com.be.web.dto.request;

import com.be.domain.entity.RecurrenceDay;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// LR-030 — createGroup() used to bind the raw JPA entity as @RequestBody,
// with the service layer doing a bare repository.save(group) — no field
// allow-list at all (unlike update(), which already copies fields
// one-by-one). A caller could set capacityLeft directly (bypasses the
// @PrePersist default-from-capacity guard) or, more seriously, reference
// an existing Enrollment's id inside a crafted `enrollments` array and
// re-parent someone else's real enrollment onto the new group on save
// (Group.enrollments is CascadeType.ALL, orphanRemoval=true). Same fix
// WorkshopCreateDTO already got: explicit fields only, ids resolved
// server-side in GroupService.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupCreateDTO {
    private String titleDe;
    private String titleEn;
    private String titleUa;
    private Integer capacity;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private Long workshopId;
    private Long teacherId;
    private Long activityId;
    private Long venueId;
    private Long ageGroupId;
    private boolean active;

    // LR-081 (LR-ADR-023) — mutually exclusive with workshopId, enforced
    // in GroupService (see its own comment). recurrenceDays/dates only
    // meaningful when courseId is set.
    private Long courseId;
    private List<RecurrenceDay> recurrenceDays;
    private LocalDate recurrenceStartDate;
    private LocalDate recurrenceEndDate;
}

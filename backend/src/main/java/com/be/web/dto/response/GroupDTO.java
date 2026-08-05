package com.be.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupDTO {
    private Long id;

    // multilingual titles (exposed for admin UI)
    private String titleDe;
    private String titleEn;
    private String titleUa;

    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;

    private Integer capacity;
    private Integer enrolledCount;

    // relations (IDs only)
    private Long workshopId;
    private String workshopTitle;
    private Long activityId;
    private Long teacherId;
    private Long ageGroupId;
    // LR-015 — human-readable label for the admin Groups list/edit form,
    // same reasoning as venueName below (composed server-side so the
    // frontend doesn't need to know the "titleDe (min–max)" format).
    private String ageGroupName;
    private Long languageId;

    // LR-015 — venue moved here from Workshop; venueName includes the
    // room, since one venues row == one physical room (e.g. "TLab29 —
    // Blauer Saal"), not a separate field the UI needs to compose.
    private Long venueId;
    private String venueName;

    private boolean active;
}
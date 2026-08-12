package com.be.web.mapper;

import com.be.domain.entity.AgeGroup;
import com.be.domain.entity.Group;
import com.be.domain.entity.Venue;
import com.be.web.dto.response.GroupDTO;
import org.springframework.stereotype.Component;

@Component
public class GroupMapper {

    public GroupDTO toDto(Group group) {
        if (group == null) return null;

        return GroupDTO.builder()
                .id(group.getId())
                // titles
                .titleDe(group.getTitleDe())
                .titleEn(group.getTitleEn())
                .titleUa(group.getTitleUa())
                // times
                .startDateTime(group.getStartDateTime())
                .endDateTime(group.getEndDateTime())
                // capacity / enrolled
                .capacity(group.getCapacity())
                .enrolledCount(group.getEnrollments() == null ? 0 : group.getEnrollments().size())
                // relations by id
                .workshopId(group.getWorkshop() != null ? group.getWorkshop().getId() : null)
                .workshopTitle(
                        group.getWorkshop() != null
                                ? group.getWorkshop().getWorkshopName()
                                : null
                )
                .activityId(group.getActivity() != null ? group.getActivity().getId() : null)
                .teacherId(group.getTeacher() != null ? group.getTeacher().getId() : null)
                .ageGroupId(group.getAgeGroup() != null ? group.getAgeGroup().getId() : null)
                .ageGroupName(group.getAgeGroup() != null ? formatAgeGroupName(group.getAgeGroup()) : null)
                .languageId(group.getLanguage() != null ? group.getLanguage().getId() : null)
                // LR-015 — venue moved here from Workshop (was previously
                // missing from this mapper entirely, unlike WorkshopMapper.
                // toGroupDTO(), so the admin Groups page always showed venue
                // as empty even though the public workshop-detail page,
                // which goes through the other mapper, showed it correctly).
                .venueId(group.getVenue() != null ? group.getVenue().getId() : null)
                .venueName(group.getVenue() != null ? formatVenueName(group.getVenue()) : null)
                .active(group.isActive())
                // LR-081 (LR-ADR-023)
                .courseId(group.getCourse() != null ? group.getCourse().getId() : null)
                .recurrenceDays(group.getRecurrenceDays())
                .recurrenceStartDate(group.getRecurrenceStartDate())
                .recurrenceEndDate(group.getRecurrenceEndDate())
                .build();
    }

    // Same one-row-per-room composition as WorkshopMapper.formatVenueName —
    // duplicated deliberately (CODING_PROTOCOL.md §2: independent call
    // sites, not worth a shared util for three lines).
    private String formatVenueName(Venue v) {
        return (v.getRoom() == null || v.getRoom().isBlank())
                ? v.getName()
                : v.getName() + " — " + v.getRoom();
    }

    // "Kinder (6–10)" — titleDe + age range, human-readable label for the
    // admin Groups list/edit form.
    private String formatAgeGroupName(AgeGroup a) {
        return a.getTitleDe() + " (" + a.getMinAge() + "–" + a.getMaxAge() + ")";
    }
}
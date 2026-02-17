package com.be.web.mapper;

import com.be.domain.entity.Group;
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
                .languageId(group.getLanguage() != null ? group.getLanguage().getId() : null)
                .active(group.isActive())
                .build();
    }
}
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
                .name(group.getTitleEn()) // или titleDe — реши стандарт
                .startDateTime(group.getStartDateTime())
                .endDateTime(group.getEndDateTime())
                .capacity(group.getCapacity())
                .enrolledCount(
                        group.getEnrollments() == null
                                ? 0
                                : group.getEnrollments().size()
                )
                .build();
    }
}
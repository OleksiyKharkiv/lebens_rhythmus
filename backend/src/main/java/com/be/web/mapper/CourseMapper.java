package com.be.web.mapper;

import com.be.domain.entity.Course;
import com.be.domain.entity.Group;
import com.be.domain.entity.enums.CourseStatus;
import com.be.web.dto.request.CourseCreateDTO;
import com.be.web.dto.response.CourseDetailDTO;
import com.be.web.dto.response.CourseListDTO;
import com.be.web.dto.response.UserBasicDTO;
import org.springframework.stereotype.Component;

@Component
public class CourseMapper {

    private final UserMapper userMapper;

    public CourseMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public CourseListDTO toListDTO(Course c) {
        UserBasicDTO teacher = c.getTeacher() != null ? userMapper.toBasicDTO(c.getTeacher()) : null;
        String shortDe = c.getDescriptionDe() != null && c.getDescriptionDe().length() > 200
                ? c.getDescriptionDe().substring(0, 200) + "..."
                : c.getDescriptionDe();
        return CourseListDTO.builder()
                .id(c.getId())
                .titleDe(c.getTitleDe())
                .titleEn(c.getTitleEn())
                .titleUa(c.getTitleUa())
                .shortDescriptionDe(shortDe)
                .teacher(teacher)
                .isOnline(c.isOnline())
                .isSynchronous(c.isSynchronous())
                .hasRecordings(c.isHasRecordings())
                .price(c.getPrice())
                .priceDescription(c.getPriceDescription())
                .status(c.getStatus() != null ? c.getStatus().name() : null)
                .build();
    }

    public CourseDetailDTO toDetailDTO(Course c) {
        return toDetailDTO(c, null);
    }

    // scheduleGroup — this Course's linked Group (LR-081, "one Course = one
    // Group" MVP scope), null if none exists yet (schedule not set up).
    public CourseDetailDTO toDetailDTO(Course c, Group scheduleGroup) {
        UserBasicDTO teacher = c.getTeacher() != null ? userMapper.toBasicDTO(c.getTeacher()) : null;
        return CourseDetailDTO.builder()
                .id(c.getId())
                .titleDe(c.getTitleDe())
                .titleEn(c.getTitleEn())
                .titleUa(c.getTitleUa())
                .descriptionDe(c.getDescriptionDe())
                .descriptionEn(c.getDescriptionEn())
                .descriptionUa(c.getDescriptionUa())
                .ageGroupId(c.getAgeGroup() != null ? c.getAgeGroup().getId() : null)
                .ageGroupName(c.getAgeGroup() != null ? c.getAgeGroup().getTitleDe() : null)
                .teacher(teacher)
                .isOnline(c.isOnline())
                .isSynchronous(c.isSynchronous())
                .hasRecordings(c.isHasRecordings())
                .formatDisclaimerDe(c.getFormatDisclaimerDe())
                .formatDisclaimerEn(c.getFormatDisclaimerEn())
                .formatDisclaimerUa(c.getFormatDisclaimerUa())
                .price(c.getPrice())
                .priceDescription(c.getPriceDescription())
                .backgroundImageUrl(c.getBackgroundImageUrl())
                .status(c.getStatus() != null ? c.getStatus().name() : null)
                .scheduleStartDate(scheduleGroup != null ? scheduleGroup.getRecurrenceStartDate() : null)
                .scheduleEndDate(scheduleGroup != null ? scheduleGroup.getRecurrenceEndDate() : null)
                .scheduleDays(scheduleGroup != null ? scheduleGroup.getRecurrenceDays() : null)
                .build();
    }

    /**
     * Creates a Course entity from the create DTO. Service must resolve
     * and set ageGroup/teacher separately (entities need fetching).
     */
    public Course fromCreateDTO(CourseCreateDTO dto) {
        Course c = new Course();
        c.setTitleDe(dto.getTitleDe());
        c.setTitleEn(dto.getTitleEn());
        c.setTitleUa(dto.getTitleUa());
        c.setDescriptionDe(dto.getDescriptionDe());
        c.setDescriptionEn(dto.getDescriptionEn());
        c.setDescriptionUa(dto.getDescriptionUa());
        c.setOnline(Boolean.TRUE.equals(dto.getIsOnline()));
        c.setSynchronous(dto.getIsSynchronous() == null || dto.getIsSynchronous());
        c.setHasRecordings(Boolean.TRUE.equals(dto.getHasRecordings()));
        c.setFormatDisclaimerDe(dto.getFormatDisclaimerDe());
        c.setFormatDisclaimerEn(dto.getFormatDisclaimerEn());
        c.setFormatDisclaimerUa(dto.getFormatDisclaimerUa());
        c.setPrice(dto.getPrice());
        c.setPriceDescription(dto.getPriceDescription());
        c.setBackgroundImageUrl(dto.getBackgroundImageUrl());
        if (dto.getStatus() != null) {
            c.setStatus(CourseStatus.valueOf(dto.getStatus()));
        }
        return c;
    }
}

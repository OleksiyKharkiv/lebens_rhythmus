package com.be.web.mapper;

import com.be.domain.entity.Course;
import com.be.domain.entity.Enrollment;
import com.be.domain.entity.Group;
import com.be.domain.entity.Order;
import com.be.domain.entity.Workshop;
import com.be.web.dto.response.EnrollmentAdminDTO;
import com.be.web.dto.response.EnrollmentResponseDTO;
import com.be.web.dto.response.UserBasicDTO;
import org.springframework.stereotype.Component;

@Component
public class EnrollmentMapper {

    private final UserMapper userMapper;

    public EnrollmentMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public EnrollmentResponseDTO toResponseDTO(Enrollment e) {
        Workshop w = e.getWorkshop();
        Course c = e.getCourse();
        Group g = e.getGroup();
        Order o = e.getOrder();

        // Maps group ID, title, and enrollment status
        return EnrollmentResponseDTO.builder()
                .id(e.getId())
                .workshopId(w != null ? w.getId() : null)
                .workshopTitle(w != null ? w.getWorkshopName() : null)
                .courseId(c != null ? c.getId() : null)
                .courseTitle(c != null ? c.getTitleDe() : null)
                .groupId(g != null ? g.getId() : null)
                .groupTitle(g != null ? selectGroupTitle(g) : null)
                .status(e.getStatus())
                .orderId(o != null ? o.getId() : null)
                .orderAmount(o != null ? o.getAmount() : null)
                .orderCurrency(o != null ? o.getCurrency() : null)
                .createdAt(e.getCreatedAt())
                .build();
    }

    public EnrollmentAdminDTO toAdminDTO(Enrollment e) {
        EnrollmentResponseDTO basic = toResponseDTO(e);
        UserBasicDTO userDto = e.getUser() != null ? userMapper.toBasicDTO(e.getUser()) : null;

        // Maps basic enrollment and workshop details
        return EnrollmentAdminDTO.builder()
                .id(basic.getId())
                .workshopId(basic.getWorkshopId())
                .workshopTitle(basic.getWorkshopTitle())
                .courseId(basic.getCourseId())
                .courseTitle(basic.getCourseTitle())
                .groupId(basic.getGroupId())
                .groupTitle(basic.getGroupTitle())
                .status(basic.getStatus())
                .createdAt(basic.getCreatedAt())
                .user(userDto)
                .build();
    }

    private String selectGroupTitle(Group g) {
        // choose an available language title, fallback order: En -> De -> Ua
        if (g.getTitleEn() != null && !g.getTitleEn().isBlank()) return g.getTitleEn();
        if (g.getTitleDe() != null && !g.getTitleDe().isBlank()) return g.getTitleDe();
        return g.getTitleUa();
    }
}
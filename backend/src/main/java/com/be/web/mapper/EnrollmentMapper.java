package com.be.web.mapper;

import com.be.domain.entity.Enrollment;
import com.be.domain.entity.Group;
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
        Group g = e.getGroup();

        return EnrollmentResponseDTO.builder()
                .id(e.getId())
                .workshopId(w != null ? w.getWorkshopId() : null)
                .workshopTitle(w != null ? w.getWorkshopName() : null)
                .groupId(g != null ? g.getId() : null)
                .groupTitle(g != null ? selectGroupTitle(g) : null)
                .status(e.getStatus())
                .createdAt(e.getCreatedAt())
                .build();
    }

    public EnrollmentAdminDTO toAdminDTO(Enrollment e) {
        EnrollmentResponseDTO basic = toResponseDTO(e);
        UserBasicDTO userDto = e.getUser() != null ? userMapper.toBasicDTO(e.getUser()) : null;

        return EnrollmentAdminDTO.builder()
                .id(basic.getId())
                .workshopId(basic.getWorkshopId())
                .workshopTitle(basic.getWorkshopTitle())
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
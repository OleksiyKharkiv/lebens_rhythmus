package com.be.web.mapper;

import com.be.domain.entity.Teacher;
import com.be.web.dto.TeacherInfoDTO;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class TeacherMapper {

    public List<TeacherInfoDTO> toInfoDTOList(List<Teacher> teachers) {
        if (teachers == null) {
            return Collections.emptyList();
        }
        return teachers.stream()
                .map(this::toInfoDTO)
                .collect(Collectors.toList());
    }

    public TeacherInfoDTO toInfoDTO(Teacher teacher) {
        if (teacher == null) {
            return null;
        }
        return TeacherInfoDTO.builder()
                .id(teacher.getId())
                .firstName(teacher.getFirstName())
                .lastName(teacher.getLastName())
                .email(teacher.getEmail())
                .phone(teacher.getPhone())
                .title(teacher.getTitle())
                .approved(teacher.isApproved())
                .bioDe(teacher.getBioDe())
                .bioEn(teacher.getBioEn())
                .bioUa(teacher.getBioUa())
                .active(teacher.isActive())
                .build();
    }

    public Teacher fromRequestDTO(com.be.web.dto.request.TeacherRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        return Teacher.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .title(dto.getTitle())
                .approved(dto.isApproved())
                .bioDe(dto.getBioDe())
                .bioEn(dto.getBioEn())
                .bioUa(dto.getBioUa())
                .active(dto.isActive())
                .build();
    }
}
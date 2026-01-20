package com.be.web.mapper;

import com.be.domain.entity.Teacher;
import com.be.web.dto.TeacherInfoDTO;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class TeacherMapper {

    /**
     * Maps teacher list to the info DTO list
     */
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
                .title(teacher.getTitle())
                .bio(teacher.getBioDe())
                .approved(teacher.isApproved())
                .build();
    }
}
package com.be.web.mapper;

import com.be.domain.entity.Group;
import com.be.domain.entity.Workshop;
import com.be.domain.entity.WorkshopFile;
import com.be.domain.entity.enums.WorkshopStatus;
import com.be.web.dto.request.WorkshopCreateDTO;
import com.be.web.dto.response.GroupDTO;
import com.be.web.dto.response.WorkshopDetailDTO;
import com.be.web.dto.response.WorkshopFileDTO;
import com.be.web.dto.response.WorkshopListDTO;
import com.be.web.dto.response.UserBasicDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class WorkshopMapper {

    private final UserMapper userMapper;

    public WorkshopMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public WorkshopListDTO toListDTO(Workshop w) {
        UserBasicDTO teacher = w.getTeacher() != null ? userMapper.toBasicDTO(w.getTeacher()) : null;
        return WorkshopListDTO.builder()
                .id(w.getId())
                .title(w.getWorkshopName())
                .shortDescription(w.getDescription() != null && w.getDescription().length() > 200
                        ? w.getDescription().substring(0, 200) + "..."
                        : w.getDescription())
                .teacher(teacher)
                .startDate(w.getStartDate())
                .endDate(w.getEndDate())
                .venueName(w.getVenue() != null ? w.getVenue().getName() : null)
                .price(w.getPrice())
                .status(String.valueOf(w.getStatus()))
                .build();
    }

    public WorkshopDetailDTO toDetailDTO(Workshop w) {
        UserBasicDTO teacher = w.getTeacher() != null ? userMapper.toBasicDTO(w.getTeacher()) : null;
        List<GroupDTO> groups = w.getGroups() == null ? List.of() : w.getGroups().stream()
                .map(this::toGroupDTO)
                .collect(Collectors.toList());
        List<WorkshopFileDTO> files = w.getFiles() == null ? List.of() : w.getFiles().stream()
                .map(this::toFileDTO)
                .collect(Collectors.toList());

        int enrollments = w.getEnrollments() == null ? 0 : w.getEnrollments().size();

        return WorkshopDetailDTO.builder()
                .id(w.getId())
                .title(w.getWorkshopName())
                .description(w.getDescription())
                .teacher(teacher)
                .startDate(w.getStartDate())
                .endDate(w.getEndDate())
                .venueName(w.getVenue() != null ? w.getVenue().getName() : null)
                .price(w.getPrice())
                .status(String.valueOf(w.getStatus()))
                .groups(groups)
                .files(files)
                .totalEnrollments(enrollments)
                .build();
    }

    private GroupDTO toGroupDTO(Group g) {
        return GroupDTO.builder()
                .id(g.getId())
                .name(g.getTitleEn())
                .startDateTime(g.getStartDateTime())
                .endDateTime(g.getEndDateTime())
                .capacity(g.getCapacity())
                .enrolledCount(g.getEnrollments() == null ? 0 : g.getEnrollments().size())
                .build();
    }

    private WorkshopFileDTO toFileDTO(WorkshopFile f) {
        return WorkshopFileDTO.builder()
                .id(f.getId())
                .filename(f.getFilename())
                .url(f.getFileUrl())
                .contentType(f.getContentType())
                .fileSize(f.getFileSize())
                .build();
    }

    public Workshop fromCreateDTO(WorkshopCreateDTO dto) {
        Workshop w = new Workshop();
        w.setWorkshopName(dto.getTitle());
        w.setDescription(dto.getDescription());
        w.setStartDate(dto.getStartDate());
        w.setEndDate(dto.getEndDate());
        w.setMaxParticipants(dto.getMaxParticipants());
        w.setPrice(dto.getPrice());
        w.setStatus(WorkshopStatus.valueOf(dto.getStatus()));
        // service must set teacher/venue (entities need fetching)
        return w;
    }
}
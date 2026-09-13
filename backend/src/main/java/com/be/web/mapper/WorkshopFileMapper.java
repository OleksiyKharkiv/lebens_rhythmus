package com.be.web.mapper;

import com.be.domain.entity.WorkshopFile;
import com.be.web.dto.request.WorkshopFileRequestDTO;
import com.be.web.dto.response.WorkshopFileDTO;
import com.be.web.dto.response.UserMediaDTO;
import org.springframework.stereotype.Component;

@Component
public class WorkshopFileMapper {

    public WorkshopFileDTO toDTO(WorkshopFile wf) {
        if (wf == null) return null;
        return WorkshopFileDTO.builder()
                .id(wf.getId())
                .filename(wf.getFilename())
                .url(wf.getFileUrl())
                .contentType(wf.getContentType())
                .fileSize(wf.getFileSize())
                .build();
    }

    public UserMediaDTO toUserMediaDTO(WorkshopFile wf) {
        if (wf == null) return null;
        return UserMediaDTO.builder()
                .id(wf.getId())
                .filename(wf.getFilename())
                .url(wf.getFileUrl())
                .contentType(wf.getContentType())
                .fileSize(wf.getFileSize())
                .workshopId(wf.getWorkshop() != null ? wf.getWorkshop().getId() : null)
                .workshopTitle(wf.getWorkshop() != null ? wf.getWorkshop().getWorkshopName() : null)
                .build();
    }

    public WorkshopFile fromRequestDTO(WorkshopFileRequestDTO dto) {
        if (dto == null) return null;
        return WorkshopFile.builder()
                .filename(dto.getFilename())
                .fileUrl(dto.getFileUrl())
                .contentType(dto.getContentType())
                .fileSize(dto.getFileSize())
                .build();
    }
}
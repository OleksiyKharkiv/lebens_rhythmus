package com.be.web.mapper;

import com.be.domain.entity.File;
import com.be.web.dto.request.FileRequestDTO;
import com.be.web.dto.response.FileResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class FileMapper {

    public FileResponseDTO toResponseDTO(File file) {
        if (file == null) return null;
        return FileResponseDTO.builder()
                .id(file.getId())
                .fileName(file.getFileName())
                .filePath(file.getFilePath())
                .fileType(file.getFileType())
                .fileSize(file.getFileSize())
                .uploadDate(file.getUploadDate())
                .workshopId(file.getWorkshop() != null ? file.getWorkshop().getId() : null)
                .build();
    }

    public File fromRequestDTO(FileRequestDTO dto) {
        if (dto == null) return null;
        return File.builder()
                .fileName(dto.getFileName())
                .filePath(dto.getFilePath())
                .fileType(dto.getFileType())
                .fileSize(dto.getFileSize())
                .build();
    }
}

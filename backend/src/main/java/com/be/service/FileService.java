package com.be.service;

import com.be.domain.entity.File;
import com.be.domain.entity.Workshop;
import com.be.domain.repository.FileRepository;
import com.be.domain.repository.WorkshopRepository;
import com.be.web.dto.request.FileRequestDTO;
import com.be.web.mapper.FileMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class FileService {

    private final FileRepository fileRepository;
    private final WorkshopRepository workshopRepository;
    private final FileMapper fileMapper;

    public FileService(FileRepository fileRepository, WorkshopRepository workshopRepository, FileMapper fileMapper) {
        this.fileRepository = fileRepository;
        this.workshopRepository = workshopRepository;
        this.fileMapper = fileMapper;
    }

    @Transactional(readOnly = true)
    public List<File> getAll() {
        return fileRepository.findAll();
    }

    @Transactional(readOnly = true)
    public File getById(Long id) {
        return fileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found with id: " + id));
    }

    @Transactional
    public File create(FileRequestDTO dto) {
        File file = fileMapper.fromRequestDTO(dto);
        if (dto.getWorkshopId() != null) {
            Workshop workshop = workshopRepository.findById(dto.getWorkshopId())
                    .orElseThrow(() -> new RuntimeException("Workshop not found with id: " + dto.getWorkshopId()));
            file.setWorkshop(workshop);
        }
        return fileRepository.save(file);
    }

    @Transactional
    public void delete(Long id) {
        if (!fileRepository.existsById(id)) {
            throw new RuntimeException("File not found with id: " + id);
        }
        fileRepository.deleteById(id);
    }
}

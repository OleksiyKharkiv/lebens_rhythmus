package com.be.service;

import com.be.domain.entity.Performance;
import com.be.domain.entity.Workshop;
import com.be.domain.entity.enums.PerformanceStatus;
import com.be.domain.repository.PerformanceRepository;
import com.be.domain.repository.WorkshopRepository;
import com.be.web.dto.request.PerformanceRequestDTO;
import com.be.web.mapper.PerformanceMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PerformanceService {

    private final PerformanceRepository performanceRepository;
    private final WorkshopRepository workshopRepository;
    private final PerformanceMapper performanceMapper;

    public PerformanceService(PerformanceRepository performanceRepository,
                              WorkshopRepository workshopRepository,
                              PerformanceMapper performanceMapper) {
        this.performanceRepository = performanceRepository;
        this.workshopRepository = workshopRepository;
        this.performanceMapper = performanceMapper;
    }

    @Transactional(readOnly = true)
    public List<Performance> getAll() {
        return performanceRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Performance getById(Long id) {
        return performanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Performance not found with id: " + id));
    }

    @Transactional
    public Performance create(PerformanceRequestDTO dto) {
        Performance performance = performanceMapper.fromRequestDTO(dto);
        if (dto.getWorkshopId() != null) {
            Workshop workshop = workshopRepository.findById(dto.getWorkshopId())
                    .orElseThrow(() -> new RuntimeException("Workshop not found"));
            performance.setWorkshop(workshop);
        }
        if (dto.getStatus() != null) {
            performance.setStatus(PerformanceStatus.valueOf(dto.getStatus()));
        }
        return performanceRepository.save(performance);
    }

    @Transactional
    public Performance update(Long id, PerformanceRequestDTO dto) {
        Performance existing = getById(id);
        
        if (dto.getTitle() != null) existing.setTitle(dto.getTitle());
        if (dto.getDescription() != null) existing.setDescription(dto.getDescription());
        if (dto.getPerformanceDate() != null) existing.setPerformanceDate(dto.getPerformanceDate());
        if (dto.getVenue() != null) existing.setVenue(dto.getVenue());
        if (dto.getMaxAttendees() != null) existing.setMaxAttendees(dto.getMaxAttendees());
        if (dto.getStatus() != null) existing.setStatus(PerformanceStatus.valueOf(dto.getStatus()));

        if (dto.getWorkshopId() != null) {
            Workshop workshop = workshopRepository.findById(dto.getWorkshopId())
                    .orElseThrow(() -> new RuntimeException("Workshop not found"));
            existing.setWorkshop(workshop);
        }

        return performanceRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        if (!performanceRepository.existsById(id)) {
            throw new RuntimeException("Performance not found");
        }
        performanceRepository.deleteById(id);
    }
}

package com.be.service;

import com.be.domain.entity.User;
import com.be.domain.entity.Workshop;
import com.be.domain.entity.enums.WorkshopStatus;
import com.be.domain.repository.UserRepository;
import com.be.domain.repository.WorkshopRepository;
import com.be.web.dto.request.WorkshopCreateDTO;
import com.be.web.mapper.WorkshopMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class WorkshopService {

    private final WorkshopRepository workshopRepository;
    private final UserRepository userRepository;
    private final WorkshopMapper workshopMapper;

    public WorkshopService(WorkshopRepository workshopRepository, UserRepository userRepository, WorkshopMapper workshopMapper) {
        this.workshopRepository = workshopRepository;
        this.userRepository = userRepository;
        this.workshopMapper = workshopMapper;
    }

    // public listing (optionally filter upcoming)
    @Transactional(readOnly = true)
    public List<Workshop> listWorkshops(boolean upcoming) {
        if (upcoming) {
            return workshopRepository.findByStartDateAfterOrderByStartDateAsc(LocalDate.now().minusDays(1));
        } else {
            return workshopRepository.findAll();
        }
    }

    @Transactional(readOnly = true)
    public Workshop getById(Long id) {
        return workshopRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workshop not found with id: " + id));
    }

    @Transactional
    public Workshop createWorkshop(WorkshopCreateDTO dto) {
        Workshop w = workshopMapper.fromCreateDTO(dto);

        if (dto.getTeacherId() != null) {
            User teacher = userRepository.findById(dto.getTeacherId())
                    .orElseThrow(() -> new RuntimeException("Teacher user not found: " + dto.getTeacherId()));
            w.setTeacher(teacher);
        }

        // venue binding should be done here (fetch Venue repo) — omitted for brevity, add if you have VenueRepository
        return workshopRepository.save(w);
    }

    @Transactional
    public Workshop updateWorkshop(Long id, WorkshopCreateDTO dto) {
        Workshop existing = getById(id);

        if (dto.getTitle() != null) existing.setWorkshopName(dto.getTitle());
        if (dto.getDescription() != null) existing.setDescription(dto.getDescription());
        if (dto.getStartDate() != null) existing.setStartDate(dto.getStartDate());
        if (dto.getEndDate() != null) existing.setEndDate(dto.getEndDate());
        if (dto.getMaxParticipants() != null) existing.setMaxParticipants(dto.getMaxParticipants());
        if (dto.getPrice() != null) existing.setPrice(dto.getPrice());
        if (dto.getStatus() != null) existing.setStatus(WorkshopStatus.valueOf(dto.getStatus()));

        if (dto.getTeacherId() != null) {
            User teacher = userRepository.findById(dto.getTeacherId())
                    .orElseThrow(() -> new RuntimeException("Teacher not found: " + dto.getTeacherId()));
            existing.setTeacher(teacher);
        }

        return workshopRepository.save(existing);
    }

    @Transactional
    public void deleteWorkshop(Long id) {
        if (!workshopRepository.existsById(id)) {
            throw new RuntimeException("Workshop not found: " + id);
        }
        workshopRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Workshop> findByTeacher(Long teacherId) {
        return workshopRepository.findByTeacherId(teacherId);
    }

}
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
import java.util.Objects;

@Service
@Transactional
public class WorkshopService {
    private final WorkshopRepository workshopRepository;
    private final UserRepository userRepository;
    private final WorkshopMapper workshopMapper;

    public WorkshopService(WorkshopRepository workshopRepository,
                           UserRepository userRepository,
                           WorkshopMapper workshopMapper) {
        this.workshopRepository = Objects.requireNonNull(workshopRepository, "workshopRepository");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.workshopMapper = Objects.requireNonNull(workshopMapper, "workshopMapper");
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
                    .orElseThrow(() -> new RuntimeException(
                            "Teacher user not found: " + dto.getTeacherId()));
            w.setTeacher(teacher);
        }

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

        // Authoritative on every update, not skip-if-null: the admin form
        // always submits the whole current state (PUT, not PATCH), so a
        // null teacherId means "explicitly cleared via the '—' option in
        // the UI", not "field omitted" — same fix as CourseService's
        // identical bug, found live in prod 2026-08-09 (the old teacher
        // silently reappeared after being cleared and saved).
        if (dto.getTeacherId() != null) {
            User teacher = userRepository.findById(dto.getTeacherId())
                    .orElseThrow(() -> new RuntimeException(
                            "Teacher not found: " + dto.getTeacherId()));
            existing.setTeacher(teacher);
        } else {
            existing.setTeacher(null);
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
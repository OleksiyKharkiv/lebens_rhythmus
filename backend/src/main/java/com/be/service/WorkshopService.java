package com.be.service;

import com.be.domain.entity.User;
import com.be.domain.entity.Venue;
import com.be.domain.entity.Workshop;
import com.be.domain.entity.enums.WorkshopStatus;
import com.be.domain.repository.UserRepository;
import com.be.domain.repository.VenueRepository;
import com.be.domain.repository.WorkshopRepository;
import com.be.web.dto.request.WorkshopCreateDTO;
import com.be.web.mapper.WorkshopMapper;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final VenueRepository venueRepository;
    private final WorkshopMapper workshopMapper;

    /**
     * Primary constructor (preferred) — Spring will use this constructor when VenueRepository is available.
     */
    @Autowired
    public WorkshopService(WorkshopRepository workshopRepository,
                           UserRepository userRepository,
                           VenueRepository venueRepository,
                           WorkshopMapper workshopMapper) {
        this.workshopRepository = Objects.requireNonNull(workshopRepository, "workshopRepository");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.venueRepository = venueRepository; // can be null only if using deprecated ctor
        this.workshopMapper = Objects.requireNonNull(workshopMapper, "workshopMapper");
    }

    /**
     * Backward-compatible constructor kept for environments / generated metadata that expect the old signature.
     * Marked Deprecated to signal migration.
     */
    @Deprecated
    public WorkshopService(WorkshopRepository workshopRepository,
                           UserRepository userRepository,
                           WorkshopMapper workshopMapper) {
        this.workshopRepository = Objects.requireNonNull(workshopRepository, "workshopRepository");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository");
        this.venueRepository = null; // explicit: not available in this runtime
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

        bindVenueIfPresent(w, dto.getVenueId());
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
                    .orElseThrow(() -> new RuntimeException(
                            "Teacher not found: " + dto.getTeacherId()));
            existing.setTeacher(teacher);
        }

        bindVenueIfPresent(existing, dto.getVenueId());

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

    private void bindVenueIfPresent(Workshop workshop, Long venueId) {
        if (venueId == null) {
            return;
        }

        if (venueRepository == null) {
            throw new IllegalStateException(
                    "VenueRepository not available in this runtime — cannot bind venue");
        }

        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new RuntimeException("Venue not found: " + venueId));

        workshop.setVenue(venue);
    }

}
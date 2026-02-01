package com.be.service;

import com.be.domain.entity.Venue;
import com.be.domain.repository.VenueRepository;
import com.be.web.dto.request.VenueRequestDTO;
import com.be.web.mapper.VenueMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class VenueService {

    private final VenueRepository venueRepository;
    private final VenueMapper venueMapper;

    public VenueService(VenueRepository venueRepository, VenueMapper venueMapper) {
        this.venueRepository = venueRepository;
        this.venueMapper = venueMapper;
    }

    @Transactional(readOnly = true)
    public List<Venue> getAll() {
        return venueRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Venue getById(Long id) {
        return venueRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Venue not found with id: " + id));
    }

    @Transactional
    public Venue create(VenueRequestDTO dto) {
        Venue venue = venueMapper.fromRequestDTO(dto);
        return venueRepository.save(venue);
    }

    @Transactional
    public Venue update(Long id, VenueRequestDTO dto) {
        Venue existing = getById(id);
        
        if (dto.getName() != null) existing.setName(dto.getName());
        if (dto.getAddress() != null) existing.setAddress(dto.getAddress());
        if (dto.getCity() != null) existing.setCity(dto.getCity());
        if (dto.getPostalCode() != null) existing.setPostalCode(dto.getPostalCode());
        if (dto.getCountry() != null) existing.setCountry(dto.getCountry());
        if (dto.getCapacity() != null) existing.setCapacity(dto.getCapacity());
        if (dto.getDescription() != null) existing.setDescription(dto.getDescription());
        if (dto.getContactPhone() != null) existing.setContactPhone(dto.getContactPhone());
        if (dto.getContactEmail() != null) existing.setContactEmail(dto.getContactEmail());

        return venueRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        if (!venueRepository.existsById(id)) {
            throw new RuntimeException("Venue not found");
        }
        venueRepository.deleteById(id);
    }
}

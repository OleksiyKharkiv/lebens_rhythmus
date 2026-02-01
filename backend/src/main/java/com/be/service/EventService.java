package com.be.service;

import com.be.domain.entity.Event;
import com.be.domain.entity.Venue;
import com.be.domain.entity.Workshop;
import com.be.domain.entity.Contract;
import com.be.domain.repository.EventRepository;
import com.be.domain.repository.VenueRepository;
import com.be.domain.repository.WorkshopRepository;
import com.be.domain.repository.ContractRepository;
import com.be.web.dto.request.EventRequestDTO;
import com.be.web.mapper.EventMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class EventService {

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final WorkshopRepository workshopRepository;
    private final ContractRepository contractRepository;
    private final EventMapper eventMapper;

    public EventService(EventRepository eventRepository,
                        VenueRepository venueRepository,
                        WorkshopRepository workshopRepository,
                        ContractRepository contractRepository,
                        EventMapper eventMapper) {
        this.eventRepository = eventRepository;
        this.venueRepository = venueRepository;
        this.workshopRepository = workshopRepository;
        this.contractRepository = contractRepository;
        this.eventMapper = eventMapper;
    }

    @Transactional(readOnly = true)
    public List<Event> getAll() {
        return eventRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Event getById(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + id));
    }

    @Transactional
    public Event create(EventRequestDTO dto) {
        Event event = eventMapper.fromRequestDTO(dto);
        linkRelations(event, dto);
        return eventRepository.save(event);
    }

    @Transactional
    public Event update(Long id, EventRequestDTO dto) {
        Event existing = getById(id);
        
        if (dto.getTitle() != null) existing.setTitle(dto.getTitle());
        if (dto.getDescription() != null) existing.setDescription(dto.getDescription());
        if (dto.getStartDateTime() != null) existing.setStartDateTime(dto.getStartDateTime());
        if (dto.getEndDateTime() != null) existing.setEndDateTime(dto.getEndDateTime());
        if (dto.getPrice() != null) existing.setPrice(dto.getPrice());
        if (dto.getCurrency() != null) existing.setCurrency(dto.getCurrency());
        if (dto.getCapacity() != null) existing.setCapacity(dto.getCapacity());
        if (dto.getStatus() != null) existing.setStatus(dto.getStatus());

        linkRelations(existing, dto);

        return eventRepository.save(existing);
    }

    private void linkRelations(Event event, EventRequestDTO dto) {
        if (dto.getVenueId() != null) {
            Venue venue = venueRepository.findById(dto.getVenueId())
                    .orElseThrow(() -> new RuntimeException("Venue not found with id: " + dto.getVenueId()));
            event.setVenue(venue);
        }
        if (dto.getWorkshopId() != null) {
            Workshop workshop = workshopRepository.findById(dto.getWorkshopId())
                    .orElseThrow(() -> new RuntimeException("Workshop not found with id: " + dto.getWorkshopId()));
            event.setWorkshop(workshop);
        }
        if (dto.getContractId() != null) {
            Contract contract = contractRepository.findById(dto.getContractId())
                    .orElseThrow(() -> new RuntimeException("Contract not found with id: " + dto.getContractId()));
            event.setContract(contract);
        }
    }

    @Transactional
    public void delete(Long id) {
        if (!eventRepository.existsById(id)) {
            throw new RuntimeException("Event not found with id: " + id);
        }
        eventRepository.deleteById(id);
    }
}

package com.be.service;

import com.be.domain.entity.Participant;
import com.be.domain.entity.Group;
import com.be.domain.repository.ParticipantRepository;
import com.be.domain.repository.GroupRepository;
import com.be.web.dto.request.ParticipantRequestDTO;
import com.be.web.mapper.ParticipantMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ParticipantService {

    private final ParticipantRepository participantRepository;
    private final GroupRepository groupRepository;
    private final ParticipantMapper participantMapper;

    public ParticipantService(ParticipantRepository participantRepository,
                              GroupRepository groupRepository,
                              ParticipantMapper participantMapper) {
        this.participantRepository = participantRepository;
        this.groupRepository = groupRepository;
        this.participantMapper = participantMapper;
    }

    @Transactional(readOnly = true)
    public List<Participant> getAll() {
        return participantRepository.findAll();
    }

    // Security check Tier 1.1 (2026-09-13) — scoped variant for a caller
    // whose real role is TEACHER, same reasoning as the repository method.
    @Transactional(readOnly = true)
    public List<Participant> getAllForTeacher(Long teacherId) {
        return participantRepository.findByGroup_Teacher_Id(teacherId);
    }

    @Transactional(readOnly = true)
    public Participant getById(Long id) {
        return participantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Participant not found with id: " + id));
    }

    @Transactional
    public Participant create(ParticipantRequestDTO dto) {
        Participant participant = participantMapper.fromRequestDTO(dto);
        if (dto.getGroupId() != null) {
            Group group = groupRepository.findById(dto.getGroupId())
                    .orElseThrow(() -> new RuntimeException("Group not found with id: " + dto.getGroupId()));
            participant.setGroup(group);
        }
        return participantRepository.save(participant);
    }

    @Transactional
    public Participant update(Long id, ParticipantRequestDTO dto) {
        Participant existing = getById(id);

        if (dto.getFirstName() != null) existing.setFirstName(dto.getFirstName());
        if (dto.getLastName() != null) existing.setLastName(dto.getLastName());
        if (dto.getEmail() != null) existing.setEmail(dto.getEmail());
        if (dto.getPhone() != null) existing.setPhone(dto.getPhone());
        if (dto.getBirthDate() != null) existing.setBirthDate(dto.getBirthDate());
        existing.setActive(dto.isActive());

        if (dto.getGroupId() != null) {
            Group group = groupRepository.findById(dto.getGroupId())
                    .orElseThrow(() -> new RuntimeException("Group not found with id: " + dto.getGroupId()));
            existing.setGroup(group);
        }

        return participantRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        if (!participantRepository.existsById(id)) {
            throw new RuntimeException("Participant not found");
        }
        participantRepository.deleteById(id);
    }
}
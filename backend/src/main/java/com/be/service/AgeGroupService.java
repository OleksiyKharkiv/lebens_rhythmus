package com.be.service;

import com.be.domain.entity.AgeGroup;
import com.be.domain.repository.AgeGroupRepository;
import com.be.web.dto.request.AgeGroupRequestDTO;
import com.be.web.mapper.AgeGroupMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AgeGroupService {

    private final AgeGroupRepository ageGroupRepository;
    private final AgeGroupMapper ageGroupMapper;

    public AgeGroupService(AgeGroupRepository ageGroupRepository, AgeGroupMapper ageGroupMapper) {
        this.ageGroupRepository = ageGroupRepository;
        this.ageGroupMapper = ageGroupMapper;
    }

    @Transactional(readOnly = true)
    public List<AgeGroup> getAll() {
        return ageGroupRepository.findAll();
    }

    @Transactional(readOnly = true)
    public AgeGroup getById(Long id) {
        return ageGroupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("AgeGroup not found with id: " + id));
    }

    @Transactional
    public AgeGroup create(AgeGroupRequestDTO dto) {
        AgeGroup ageGroup = ageGroupMapper.fromRequestDTO(dto);
        return ageGroupRepository.save(ageGroup);
    }

    @Transactional
    public AgeGroup update(Long id, AgeGroupRequestDTO dto) {
        AgeGroup existing = getById(id);
        
        if (dto.getTitleDe() != null) existing.setTitleDe(dto.getTitleDe());
        if (dto.getTitleEn() != null) existing.setTitleEn(dto.getTitleEn());
        if (dto.getTitleUa() != null) existing.setTitleUa(dto.getTitleUa());
        existing.setMinAge(dto.getMinAge());
        existing.setMaxAge(dto.getMaxAge());

        return ageGroupRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        if (!ageGroupRepository.existsById(id)) {
            throw new RuntimeException("AgeGroup not found with id: " + id);
        }
        ageGroupRepository.deleteById(id);
    }
}

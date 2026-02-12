package com.be.service;

import com.be.domain.entity.Language;
import com.be.domain.repository.LanguageRepository;
import com.be.web.dto.request.LanguageRequestDTO;
import com.be.web.mapper.LanguageMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class LanguageService {

    private final LanguageRepository languageRepository;
    private final LanguageMapper languageMapper;

    public LanguageService(LanguageRepository languageRepository, LanguageMapper languageMapper) {
        this.languageRepository = languageRepository;
        this.languageMapper = languageMapper;
    }

    @Transactional(readOnly = true)
    public List<Language> getAll() {
        return languageRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Language getById(Long id) {
        return languageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Language not found with id: " + id));
    }

    @Transactional
    public Language create(LanguageRequestDTO dto) {
        Language language = languageMapper.fromRequestDTO(dto);
        return languageRepository.save(language);
    }

    @Transactional
    public Language update(Long id, LanguageRequestDTO dto) {
        Language existing = getById(id);

        if (dto.getNameDe() != null) existing.setNameDe(dto.getNameDe());
        if (dto.getNameEn() != null) existing.setNameEn(dto.getNameEn());
        if (dto.getNameUa() != null) existing.setNameUa(dto.getNameUa());
        if (dto.getCode() != null) existing.setCode(dto.getCode());
        existing.setActive(dto.isActive());

        return languageRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        if (!languageRepository.existsById(id)) {
            throw new RuntimeException("Language not found with id: " + id);
        }
        languageRepository.deleteById(id);
    }
}
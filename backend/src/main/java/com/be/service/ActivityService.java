package com.be.service;

import com.be.domain.entity.Activity;
import com.be.domain.repository.ActivityRepository;
import com.be.web.dto.request.ActivityRequestDTO;
import com.be.web.mapper.ActivityMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final ActivityMapper activityMapper;

    public ActivityService(ActivityRepository activityRepository, ActivityMapper activityMapper) {
        this.activityRepository = activityRepository;
        this.activityMapper = activityMapper;
    }

    @Transactional(readOnly = true)
    public List<Activity> getAllActivities() {
        return activityRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Activity getActivityById(Long id) {
        return activityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Activity not found with id: " + id));
    }

    @Transactional
    public Activity createActivity(ActivityRequestDTO dto) {
        Activity activity = activityMapper.fromRequestDTO(dto);
        return activityRepository.save(activity);
    }

    @Transactional
    public Activity updateActivity(Long id, ActivityRequestDTO dto) {
        Activity existing = getActivityById(id);

        if (dto.getTitleDe() != null) existing.setTitleDe(dto.getTitleDe());
        if (dto.getTitleEn() != null) existing.setTitleEn(dto.getTitleEn());
        if (dto.getTitleUa() != null) existing.setTitleUa(dto.getTitleUa());
        if (dto.getDescriptionDe() != null) existing.setDescriptionDe(dto.getDescriptionDe());
        if (dto.getDescriptionEn() != null) existing.setDescriptionEn(dto.getDescriptionEn());
        if (dto.getDescriptionUa() != null) existing.setDescriptionUa(dto.getDescriptionUa());
        if (dto.getPrice() != null) existing.setPrice(dto.getPrice());
        existing.setDurationMinutes(dto.getDurationMinutes());
        existing.setActive(dto.isActive());

        return activityRepository.save(existing);
    }

    @Transactional
    public void deleteActivity(Long id) {
        if (!activityRepository.existsById(id)) {
            throw new RuntimeException("Activity not found with id: " + id);
        }
        activityRepository.deleteById(id);
    }
}
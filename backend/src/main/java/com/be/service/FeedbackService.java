package com.be.service;

import com.be.domain.entity.Feedback;
import com.be.domain.entity.User;
import com.be.domain.repository.FeedbackRepository;
import com.be.domain.repository.UserRepository;
import com.be.web.dto.request.FeedbackRequestDTO;
import com.be.web.mapper.FeedbackMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final FeedbackMapper feedbackMapper;

    public FeedbackService(FeedbackRepository feedbackRepository,
                           UserRepository userRepository,
                           FeedbackMapper feedbackMapper) {
        this.feedbackRepository = feedbackRepository;
        this.userRepository = userRepository;
        this.feedbackMapper = feedbackMapper;
    }

    @Transactional(readOnly = true)
    public List<Feedback> getAll() {
        return feedbackRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Feedback getById(Long id) {
        return feedbackRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Feedback not found with id: " + id));
    }

    @Transactional
    public Feedback create(FeedbackRequestDTO dto, Long userId) {
        Feedback feedback = feedbackMapper.fromRequestDTO(dto);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        feedback.setUser(user);

        return feedbackRepository.save(feedback);
    }

    @Transactional
    public void delete(Long id) {
        if (!feedbackRepository.existsById(id)) {
            throw new RuntimeException("Feedback not found with id: " + id);
        }
        feedbackRepository.deleteById(id);
    }
}

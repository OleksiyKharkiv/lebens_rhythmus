package com.be.service;

import com.be.domain.entity.User;
import com.be.domain.entity.enums.Role;
import com.be.domain.repository.UserRepository;
import com.be.web.dto.request.UserUpdateDTO;
import com.be.web.dto.response.UserProfileDTO;
import com.be.web.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    // ========== CRUD OPERATIONS ==========

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public User save(User user) {
        // ensure defaults for timestamps and country only
        normalizeDefaults(user);
        return userRepository.save(user);
    }

    public User createUser(User user) {
        normalizeDefaults(user);

        if (user.getPassword() == null) {
            throw new IllegalArgumentException("Password must be provided");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        if (user.getRole() == null) {
            user.setRole(Role.USER);
        }

        return userRepository.save(user);
    }

    private void normalizeDefaults(User user) {
        if (user.getCreatedAt() == null) user.setCreatedAt(LocalDateTime.now());
        if (user.getCountry() == null) user.setCountry("Deutschland");
        // boolean поля теперь примитивные, null-проверки не нужны
    }

    public User updateUser(Long userId, UserUpdateDTO updateDTO) {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        userMapper.updateEntityFromDTO(updateDTO, existingUser);
        return userRepository.save(existingUser);
    }

    public void deleteUser(Long userId) {
        userRepository.deactivateUser(userId);
    }

    // ========== SECURITY OPERATIONS ==========

    public void incrementFailedLoginAttempts(String email) {
        userRepository.incrementFailedLoginAttempts(email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getFailedLoginAttempts() >= 5) {
            userRepository.lockUserAccount(email, LocalDateTime.now().plusMinutes(15));
        }
    }

    public void resetFailedLoginAttempts(String email) {
        userRepository.resetFailedLoginAttempts(email);
    }

    public boolean isAccountLocked(String email) {
        return userRepository.findByEmail(email)
                .map(user -> user.getLockUntil() != null && user.getLockUntil().isAfter(LocalDateTime.now()))
                .orElse(false);
    }

    // ========== BUSINESS OPERATIONS ==========

    public UserProfileDTO getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        UserProfileDTO profileDTO = userMapper.toProfileDTO(user);

        profileDTO.setTotalOrders(0);
        profileDTO.setActiveParticipants(0);
        profileDTO.setHasTeacherProfile(false);

        return profileDTO;
    }

    public boolean changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return false;
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return true;
    }

    public void verifyEmail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        user.setEmailVerified(true);
        userRepository.save(user);
    }

    // ========== ADMIN OPERATIONS ==========

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> getUsersByRole(Role role) {
        return userRepository.findByRole(role);
    }

    public List<User> searchUsers(String searchTerm) {
        return userRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(searchTerm, searchTerm);
    }

    public User updateUserRole(Long userId, Role newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        user.setRole(newRole);
        return userRepository.save(user);
    }

    // ========== STATISTICS ==========

    public long getTotalUserCount() {
        return userRepository.count();
    }

    public long getActiveUserCount() {
        return userRepository.countByEnabledTrue();
    }

    public long getUserCountByRole(Role role) {
        return userRepository.countByRole(role);
    }
}

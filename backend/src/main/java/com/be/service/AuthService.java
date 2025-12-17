package com.be.service;

import com.be.config.JwtUtils;
import com.be.domain.entity.User;
import com.be.web.dto.request.UserLoginRequestDTO;
import com.be.web.dto.request.UserRegistrationDTO;
import com.be.web.dto.response.UserLoginResponseDTO;
import com.be.web.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;

    public UserLoginResponseDTO authenticate(UserLoginRequestDTO dto) {
        Optional<User> maybeUser = userService.findByEmail(dto.getEmail());
        if (maybeUser.isEmpty()) {
            throw new BadCredentialsException("Invalid credentials");
        }
        User user = maybeUser.get();

        if (user.getLockUntil() != null && user.getLockUntil().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("Account locked. Try again later.");
        }

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            userService.incrementFailedLoginAttempts(user.getEmail());
            throw new BadCredentialsException("Invalid credentials");
        }

        userService.resetFailedLoginAttempts(user.getEmail());

        String token = jwtUtils.generateToken(user);
        long expiresInSec = jwtUtils.getExpirationTime();

        return userMapper.toLoginResponseDTO(user, token, expiresInSec,
                Collections.emptyList(), Collections.emptyList());
    }

    @Transactional
    public UserLoginResponseDTO register(UserRegistrationDTO dto) {
        if (userService.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = userMapper.toEntity(dto);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setEnabled(true);
        user.setFailedLoginAttempts(0);
        user.setAcceptedTerms(true);
        user.setPrivacyPolicyAccepted(true);

        User saved = userService.createUser(user);

        String token = jwtUtils.generateToken(saved);
        long expiresInSec = jwtUtils.getExpirationTime();

        return userMapper.toLoginResponseDTO(saved, token, expiresInSec,
                Collections.emptyList(), Collections.emptyList());
    }
}
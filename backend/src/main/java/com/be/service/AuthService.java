package com.be.service;

import com.be.config.JwtUtils;
import com.be.domain.entity.User;
import com.be.web.dto.request.UserLoginRequestDTO;
import com.be.web.dto.request.UserRegistrationDTO;
import com.be.web.dto.response.UserLoginResponseDTO;
import com.be.web.mapper.UserMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;

    public AuthService(UserService userService, PasswordEncoder passwordEncoder,
                       JwtUtils jwtUtils, AuthenticationManager authenticationManager,
                       UserMapper userMapper) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.authenticationManager = authenticationManager;
        this.userMapper = userMapper;
    }

    public UserLoginResponseDTO authenticate(UserLoginRequestDTO loginRequest) {
        try {
            // Check account lock status
            User user = userService.findByEmail(loginRequest.getEmail())
                    .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

            if (user.getLockUntil() != null && user.getLockUntil().isAfter(java.time.LocalDateTime.now())) {
                throw new RuntimeException("Account locked. Try again later.");
            }

            // Authentication through Spring Security
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );
            // Reset failed login attempts counter
            userService.resetFailedLoginAttempts(user.getEmail());

            String token = jwtUtils.generateToken(user);

            return userMapper.toLoginResponseDTO(user, token, 86400L, Collections.emptyList(), Collections.emptyList());

        } catch (BadCredentialsException e) {
            // Increment failed login attempts counter
            userService.incrementFailedLoginAttempts(loginRequest.getEmail());
            throw new RuntimeException("Invalid credentials");
        }
    }

    public UserLoginResponseDTO register(UserRegistrationDTO registrationDTO) {
        // Check if a user exists
        if (userService.existsByEmail(registrationDTO.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // Create user
        User user = userMapper.toEntity(registrationDTO);
        user.setPassword(passwordEncoder.encode(registrationDTO.getPassword()));

        User savedUser = userService.save(user);

        String token = jwtUtils.generateToken(savedUser);

        return userMapper.toLoginResponseDTO(savedUser, token, 86400L, Collections.emptyList(), Collections.emptyList());
    }
}
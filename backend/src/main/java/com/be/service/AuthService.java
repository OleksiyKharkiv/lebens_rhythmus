package com.be.service;

import com.be.config.JwtUtils;
import com.be.domain.entity.User;
import com.be.web.dto.request.UserLoginRequestDTO;
import com.be.web.dto.request.UserRegistrationDTO;
import com.be.web.dto.response.UserLoginResponseDTO;
import com.be.web.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserService userService;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;

    public AuthService(UserService userService,
//                       PasswordEncoder passwordEncoder,
                       JwtUtils jwtUtils,
                       AuthenticationManager authenticationManager,
                       UserMapper userMapper) {
        this.userService = userService;
        this.jwtUtils = jwtUtils;
        this.authenticationManager = authenticationManager;
        this.userMapper = userMapper;
    }

    /**
     * Authenticate user by email/password.
     * Increments failed attempts on bad credentials and resets on success.
     */
    public UserLoginResponseDTO authenticate(UserLoginRequestDTO loginRequest) {
        try {
            Optional<User> maybeUser = userService.findByEmail(loginRequest.getEmail());

            if (maybeUser.isEmpty()) {
                // uniform message for security
                throw new BadCredentialsException("Invalid credentials");
            }

            User user = maybeUser.get();

            // account lock check
            if (user.getLockUntil() != null && user.getLockUntil().isAfter(LocalDateTime.now())) {
                throw new RuntimeException("Account locked. Try again later.");
            }

            // delegate to AuthenticationManager (will check password)
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            // successful authentication -> reset counters
            try {
                userService.resetFailedLoginAttempts(user.getEmail());
            } catch (Exception e) {
                log.warn("Failed to reset failedLoginAttempts for {}: {}", user.getEmail(), e.getMessage());
            }

            String token = jwtUtils.generateToken(user);

            return userMapper.toLoginResponseDTO(user, token, 86400L, Collections.emptyList(), Collections.emptyList());

        } catch (BadCredentialsException ex) {
            // increment failed attempts safely (user may not exist)
            try {
                userService.incrementFailedLoginAttempts(loginRequest.getEmail());
            } catch (Exception e) {
                log.warn("Failed to increment failedLoginAttempts for {}: {}", loginRequest.getEmail(), e.getMessage());
            }
            throw new RuntimeException("Invalid credentials");
        } catch (RuntimeException ex) {
            // pass through business exceptions (account locked, etc.)
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error during authentication for {}: {}", loginRequest.getEmail(), ex.getMessage(), ex);
            throw new RuntimeException("Authentication failed");
        }
    }

    /**
     * Register new user.
     * Uses UserService.createUser(...) which handles password encoding and defaults.
     */
    @Transactional
    public UserLoginResponseDTO register(UserRegistrationDTO registrationDTO) {
        // basic DTO-level checks (defensive)
        if (registrationDTO.getEmail() == null || registrationDTO.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (registrationDTO.getPassword() == null || registrationDTO.getPassword().length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }
        if (!Boolean.TRUE.equals(registrationDTO.getAcceptedTerms())) {
            throw new IllegalArgumentException("You must accept terms and conditions");
        }
        if (!Boolean.TRUE.equals(registrationDTO.getPrivacyPolicyAccepted())) {
            throw new IllegalArgumentException("You must accept the privacy policy");
        }

        if (userService.existsByEmail(registrationDTO.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = userMapper.toEntity(registrationDTO);
        user.setEnabled(true);
        user.setFailedLoginAttempts(0);
        user.setAcceptedTerms(Boolean.TRUE);
        user.setPrivacyPolicyAccepted(Boolean.TRUE);

        // ensure server-side defaults (extra safety)
        if (user.getEnabled() == null) user.setEnabled(true);
        if (user.getEmailVerified() == null) user.setEmailVerified(false);
        if (user.getAcceptedTerms() == null) user.setAcceptedTerms(true);
        if (user.getPrivacyPolicyAccepted() == null) user.setPrivacyPolicyAccepted(true);

        // createUser handles password encoding and role defaulting
        User savedUser;
        try {
            savedUser = userService.createUser(user);
        } catch (DataIntegrityViolationException dive) {
            log.warn("DataIntegrityViolation during registration for {}: {}", registrationDTO.getEmail(), dive.getMessage());
            throw new IllegalArgumentException("Invalid registration data or email already exists");
        } catch (Exception ex) {
            log.error("Unexpected error during registration for {}: {}", registrationDTO.getEmail(), ex.getMessage(), ex);
            throw new RuntimeException("Registration failed");
        }

        // generate token
        String token = jwtUtils.generateToken(savedUser);

        return userMapper.toLoginResponseDTO(savedUser, token, 86400L, Collections.emptyList(), Collections.emptyList());
    }
}
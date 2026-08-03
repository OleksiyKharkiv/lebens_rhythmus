package com.be.service;

import com.be.config.JwtUtils;
import com.be.domain.entity.User;
import com.be.domain.exception.EmailNotVerifiedException;
import com.be.web.dto.request.UserLoginRequestDTO;
import com.be.web.dto.request.UserRegistrationDTO;
import com.be.web.dto.response.RegistrationResponseDTO;
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
    private final EmailVerificationService emailVerificationService;

    /**
     * Authenticates user; returns token on valid credentials
     */
    public UserLoginResponseDTO authenticate(UserLoginRequestDTO dto) {
        Optional<User> maybeUser = userService.findByEmail(dto.getEmail());
        if (maybeUser.isEmpty()) {
            throw new BadCredentialsException("Invalid credentials");
        }
        User user = maybeUser.get();

        // Throws if account is temporarily locked
        if (user.getLockUntil() != null && user.getLockUntil().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("Account locked. Try again later.");
        }

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            userService.incrementFailedLoginAttempts(user.getEmail());
            throw new BadCredentialsException("Invalid credentials");
        }

        // Checked AFTER the password match, deliberately — revealing
        // "unverified" before the password is confirmed would let anyone
        // probe whether an email is registered/verified without knowing
        // the password at all.
        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException("Please verify your email before logging in");
        }

        userService.resetFailedLoginAttempts(user.getEmail());

        String token = jwtUtils.generateToken(user);
        long expiresInSec = jwtUtils.getExpirationTime();

        return userMapper.toLoginResponseDTO(user, token, expiresInSec,
                Collections.emptyList(), Collections.emptyList());
    }

    /**
     * Registers user and emails a verification link — no longer logs the
     * user in immediately, since login now requires a verified email.
     */
    @Transactional
    public RegistrationResponseDTO register(UserRegistrationDTO dto) {
        if (userService.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Password hashing is owned by UserService.createUser() (enforces non-null +
        // encodes once) — do not re-encode here, see KNOWN_ISSUES.md.
        User user = userMapper.toEntity(dto);
        user.setEnabled(true);
        user.setFailedLoginAttempts(0);
        user.setAcceptedTerms(true);
        user.setPrivacyPolicyAccepted(true);

        User saved = userService.createUser(user);

        emailVerificationService.sendVerificationEmail(saved);

        return RegistrationResponseDTO.builder()
                .message("Please check your email to confirm your account.")
                .email(saved.getEmail())
                .build();
    }

    /**
     * Confirms the link clicked from the verification email.
     */
    public void verifyEmail(String token) {
        emailVerificationService.verifyToken(token);
    }

    /**
     * Re-sends the verification email. Always succeeds from the caller's
     * point of view regardless of whether the email exists or is already
     * verified — revealing that would let anyone enumerate registered
     * accounts by probing this endpoint.
     */
    public void resendVerification(String email) {
        userService.findByEmail(email)
                .filter(user -> !user.isEmailVerified())
                .ifPresent(emailVerificationService::sendVerificationEmail);
    }
}
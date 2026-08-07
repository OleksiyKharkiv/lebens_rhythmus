package com.be.service;

import com.be.config.JwtUtils;
import com.be.domain.entity.User;
import com.be.domain.exception.EmailNotVerifiedException;
import com.be.web.dto.request.UserLoginRequestDTO;
import com.be.web.dto.request.UserRegistrationDTO;
import com.be.web.dto.response.RegistrationResponseDTO;
import com.be.web.dto.response.UserLoginResponseDTO;
import com.be.web.mapper.UserMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

@Slf4j
@Service
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;
    private final EmailVerificationService emailVerificationService;
    private final long resendVerificationMinResponseMs;
    private final MeterRegistry meterRegistry;

    // Explicit constructor (not @RequiredArgsConstructor) — needed for the
    // @Value below, same reasoning as EmailVerificationService's constructor.
    public AuthService(
            UserService userService,
            PasswordEncoder passwordEncoder,
            UserMapper userMapper,
            JwtUtils jwtUtils,
            EmailVerificationService emailVerificationService,
            @Value("${app.email-verification.resend-min-response-ms:400}") long resendVerificationMinResponseMs,
            MeterRegistry meterRegistry) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.jwtUtils = jwtUtils;
        this.emailVerificationService = emailVerificationService;
        this.resendVerificationMinResponseMs = resendVerificationMinResponseMs;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Authenticates user; returns token on valid credentials
     * <p>
     * LR-031 Phase 1 — each failure branch increments auth.login.failure
     * with a `reason` tag, even though the client-facing exception/message
     * stays identical across all of them (LR-014/LR-026's anti-enumeration
     * property is about the HTTP response, not server-side observability —
     * knowing internally *why* a login failed doesn't leak anything to the
     * caller). A sustained spike in one reason is exactly the signal that
     * would have made LR-023's live exploit attempt visible in real time,
     * instead of sitting undetected for ~9.5 months.
     */
    public UserLoginResponseDTO authenticate(UserLoginRequestDTO dto) {
        Optional<User> maybeUser = userService.findByEmail(dto.getEmail());
        if (maybeUser.isEmpty()) {
            meterRegistry.counter("auth.login.failure", "reason", "unknown_email").increment();
            throw new BadCredentialsException("Invalid credentials");
        }
        User user = maybeUser.get();

        // LR-026 — used to throw a bare RuntimeException here, which fell
        // through to the generic catch-all as a 500 with a distinct body
        // ("Account locked...") from bad-credentials' 401 ("Invalid
        // credentials") — a content-based oracle letting anyone who drives
        // an account into lockout (trivial: 5 wrong-password attempts,
        // no real credentials needed) then tell "this account exists and
        // is locked" apart from "wrong credentials or unknown email".
        // Same BadCredentialsException, same message, as every other
        // reason this method can fail before a real session is issued —
        // indistinguishable by design, matching LR-014's reasoning for
        // /auth/resend-verification.
        if (user.getLockUntil() != null && user.getLockUntil().isAfter(LocalDateTime.now())) {
            meterRegistry.counter("auth.login.failure", "reason", "locked").increment();
            throw new BadCredentialsException("Invalid credentials");
        }

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            userService.incrementFailedLoginAttempts(user.getEmail());
            meterRegistry.counter("auth.login.failure", "reason", "bad_password").increment();
            throw new BadCredentialsException("Invalid credentials");
        }

        // Checked AFTER the password match, deliberately — revealing
        // "unverified" before the password is confirmed would let anyone
        // probe whether an email is registered/verified without knowing
        // the password at all.
        if (!user.isEmailVerified()) {
            meterRegistry.counter("auth.login.failure", "reason", "email_not_verified").increment();
            throw new EmailNotVerifiedException("Please verify your email before logging in");
        }

        userService.resetFailedLoginAttempts(user.getEmail());

        String token = jwtUtils.generateToken(user);
        long expiresInSec = jwtUtils.getExpirationTime();

        meterRegistry.counter("auth.login.success").increment();

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
        meterRegistry.counter("auth.register").increment();

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
     * accounts by probing this endpoint. Response time is normalized too
     * (LR-014): without this, the branch that skips (unknown/already-
     * verified email, one cheap findByEmail) returns near-instantly while
     * the branch that sends (token generation + DB write + synchronous
     * SMTP) takes noticeably longer — a timing side-channel that leaks the
     * same "does this unverified account exist" fact the response body is
     * deliberately silent about.
     */
    public void resendVerification(String email) {
        long start = System.currentTimeMillis();
        userService.findByEmail(email)
                .filter(user -> !user.isEmailVerified())
                .ifPresent(emailVerificationService::sendVerificationEmail);
        padToMinResponseTime(start);
    }

    private void padToMinResponseTime(long startMillis) {
        long remaining = resendVerificationMinResponseMs - (System.currentTimeMillis() - startMillis);
        if (remaining > 0) {
            try {
                Thread.sleep(remaining);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
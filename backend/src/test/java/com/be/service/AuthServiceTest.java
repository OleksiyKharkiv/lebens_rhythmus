package com.be.service;

import com.be.config.JwtUtils;
import com.be.domain.entity.User;
import com.be.domain.entity.enums.Role;
import com.be.domain.exception.EmailNotVerifiedException;
import com.be.web.dto.request.UserLoginRequestDTO;
import com.be.web.dto.request.UserRegistrationDTO;
import com.be.web.dto.response.RegistrationResponseDTO;
import com.be.web.dto.response.UserLoginResponseDTO;
import com.be.web.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Login now blocks unverified accounts (email verification, added after
 * registration was found to hand out a working session with no way to
 * confirm the address actually belongs to the registrant). The two things
 * worth proving with tests, not just reading the code: (1) verification is
 * only ever revealed to someone who already has the right password — never
 * to a bare email-existence probe, and (2) registration no longer hands
 * back a session at all.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserService userService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserMapper userMapper;
    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private EmailVerificationService emailVerificationService;

    // 50ms — small enough to keep the suite fast, large enough for
    // resendVerification_*'s timing assertions to not be flaky against
    // System.currentTimeMillis() jitter.
    private static final long TEST_MIN_RESPONSE_MS = 50;

    private AuthService authService() {
        return new AuthService(userService, passwordEncoder, userMapper, jwtUtils, emailVerificationService,
                TEST_MIN_RESPONSE_MS);
    }

    @Test
    void authenticate_unverifiedEmail_correctPassword_throwsEmailNotVerified() {
        User user = User.builder().email("a@example.com").password("hashed").role(Role.USER)
                .emailVerified(false).build();
        when(userService.findByEmail("a@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("rightpw", "hashed")).thenReturn(true);

        UserLoginRequestDTO dto = UserLoginRequestDTO.builder().email("a@example.com").password("rightpw").build();

        assertThatThrownBy(() -> authService().authenticate(dto))
                .isInstanceOf(EmailNotVerifiedException.class);

        // Must not have gotten as far as issuing a token.
        verify(jwtUtils, never()).generateToken(any());
    }

    @Test
    void authenticate_unverifiedEmail_wrongPassword_throwsBadCredentials_notEmailNotVerified() {
        // Security property: verification status must never leak to someone
        // who doesn't already know the correct password — otherwise anyone
        // could probe "is this email registered and unverified" for free.
        User user = User.builder().email("a@example.com").password("hashed").role(Role.USER)
                .emailVerified(false).build();
        when(userService.findByEmail("a@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpw", "hashed")).thenReturn(false);

        UserLoginRequestDTO dto = UserLoginRequestDTO.builder().email("a@example.com").password("wrongpw").build();

        assertThatThrownBy(() -> authService().authenticate(dto))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void authenticate_verifiedEmail_correctPassword_returnsToken() {
        User user = User.builder().email("a@example.com").password("hashed").role(Role.USER)
                .emailVerified(true).build();
        when(userService.findByEmail("a@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("rightpw", "hashed")).thenReturn(true);
        when(jwtUtils.generateToken(user)).thenReturn("jwt-token");
        when(jwtUtils.getExpirationTime()).thenReturn(3600L);
        when(userMapper.toLoginResponseDTO(eq(user), eq("jwt-token"), eq(3600L), any(), any()))
                .thenReturn(UserLoginResponseDTO.builder().token("jwt-token").build());

        UserLoginRequestDTO dto = UserLoginRequestDTO.builder().email("a@example.com").password("rightpw").build();

        UserLoginResponseDTO result = authService().authenticate(dto);

        assertThat(result.getToken()).isEqualTo("jwt-token");
    }

    @Test
    void register_sendsVerificationEmail_andDoesNotReturnASessionToken() {
        UserRegistrationDTO dto = UserRegistrationDTO.builder()
                .email("new@example.com").password("password123")
                .firstName("New").lastName("User").build();
        User mappedUser = User.builder().email("new@example.com").password("password123").build();
        User savedUser = User.builder().id(1L).email("new@example.com").password("hashed").build();

        when(userService.existsByEmail("new@example.com")).thenReturn(false);
        when(userMapper.toEntity(dto)).thenReturn(mappedUser);
        when(userService.createUser(mappedUser)).thenReturn(savedUser);

        RegistrationResponseDTO result = authService().register(dto);

        assertThat(result.getEmail()).isEqualTo("new@example.com");
        verify(emailVerificationService).sendVerificationEmail(savedUser);
        verify(jwtUtils, never()).generateToken(any());
    }

    @Test
    void resendVerification_unknownEmail_doesNotThrow_doesNotSendEmail() {
        // No user-enumeration: the caller can't tell "unknown email" apart
        // from "already verified" apart from "sent" — all look the same.
        when(userService.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        authService().resendVerification("nobody@example.com");

        verify(emailVerificationService, never()).sendVerificationEmail(any());
    }

    @Test
    void resendVerification_unknownEmail_stillTakesAtLeastMinResponseTime() {
        // LR-014 — the branch that skips sending must not return
        // near-instantly, or its speed alone re-introduces the same
        // enumeration signal the response body is deliberately silent about.
        when(userService.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        long start = System.currentTimeMillis();
        authService().resendVerification("nobody@example.com");
        long elapsed = System.currentTimeMillis() - start;

        assertThat(elapsed).isGreaterThanOrEqualTo(TEST_MIN_RESPONSE_MS);
    }

    @Test
    void resendVerification_alreadyVerified_doesNotSendEmail() {
        User verified = User.builder().email("a@example.com").emailVerified(true).build();
        when(userService.findByEmail("a@example.com")).thenReturn(Optional.of(verified));

        authService().resendVerification("a@example.com");

        verify(emailVerificationService, never()).sendVerificationEmail(any());
    }

    @Test
    void resendVerification_unverified_sendsEmail() {
        User unverified = User.builder().email("a@example.com").emailVerified(false).build();
        when(userService.findByEmail("a@example.com")).thenReturn(Optional.of(unverified));

        authService().resendVerification("a@example.com");

        verify(emailVerificationService).sendVerificationEmail(unverified);
    }
}

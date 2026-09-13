package com.be.web.controller;

import com.be.domain.entity.enums.Role;
import com.be.service.AuthService;
import com.be.web.dto.request.UserLoginRequestDTO;
import com.be.web.dto.response.UserLoginResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for LR-108 AuthController login and logout cookie behavior.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(authService);
    }

    @Test
    @DisplayName("login: returns UserLoginResponseDTO and sets HttpOnly SameSite=Lax authToken cookie")
    void login_setsHttpOnlyCookie_andReturnsDto() {
        UserLoginResponseDTO loginResponse = UserLoginResponseDTO.builder()
                .token("jwt.token.here")
                .tokenType("Bearer")
                .expiresIn(86400L)
                .id(1L)
                .email("user@example.com")
                .role(Role.USER)
                .build();

        when(authService.authenticate(any(UserLoginRequestDTO.class))).thenReturn(loginResponse);

        UserLoginRequestDTO requestDTO = new UserLoginRequestDTO();
        requestDTO.setEmail("user@example.com");
        requestDTO.setPassword("Password123!");

        MockHttpServletRequest request = new MockHttpServletRequest();

        ResponseEntity<UserLoginResponseDTO> response = authController.login(requestDTO, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getToken()).isEqualTo("jwt.token.here");

        String setCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).isNotNull();
        assertThat(setCookie).contains("authToken=jwt.token.here");
        assertThat(setCookie).contains("HttpOnly");
        assertThat(setCookie).contains("SameSite=Lax");
        assertThat(setCookie).contains("Max-Age=86400");
        assertThat(setCookie).contains("Path=/");
    }

    @Test
    @DisplayName("login: sets Secure cookie when X-Forwarded-Proto is https")
    void login_withXForwardedProto_setsSecureCookie() {
        UserLoginResponseDTO loginResponse = UserLoginResponseDTO.builder()
                .token("jwt.token.here")
                .expiresIn(3600L)
                .build();

        when(authService.authenticate(any(UserLoginRequestDTO.class))).thenReturn(loginResponse);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-Proto", "https");

        ResponseEntity<UserLoginResponseDTO> response = authController.login(new UserLoginRequestDTO(), request);

        String setCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).isNotNull();
        assertThat(setCookie).contains("Secure");
    }

    @Test
    @DisplayName("logout: returns 200 OK and emits Set-Cookie with Max-Age=0 to clear authToken")
    void logout_setsMaxAgeZeroCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-Proto", "https");

        ResponseEntity<Void> response = authController.logout(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        String setCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).isNotNull();
        assertThat(setCookie).contains("authToken=");
        assertThat(setCookie).contains("Max-Age=0");
        assertThat(setCookie).contains("HttpOnly");
        assertThat(setCookie).contains("SameSite=Lax");
        assertThat(setCookie).contains("Path=/");
    }
}

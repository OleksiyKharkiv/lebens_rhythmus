package com.be.web.controller;

import com.be.service.AuthService;
import com.be.web.dto.request.UserLoginRequestDTO;
import com.be.web.dto.request.UserRegistrationDTO;
import com.be.web.dto.response.UserLoginResponseDTO;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserLoginResponseDTO> login(@Valid @RequestBody UserLoginRequestDTO loginRequest,
                                                      HttpServletRequest request) {
        log.debug("POST /api/auth/login Origin={}", request.getHeader("Origin"));
        UserLoginResponseDTO response = authService.authenticate(loginRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserLoginResponseDTO> register(@Valid @RequestBody UserRegistrationDTO registrationDTO,
                                                         HttpServletRequest request) {
        log.debug("POST /api/auth/register Origin={}", request.getHeader("Origin"));
        UserLoginResponseDTO response = authService.register(registrationDTO);
        return ResponseEntity.ok(response);
    }
}
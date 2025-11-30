package com.be.web.controller;

import com.be.service.AuthService;
import com.be.web.dto.request.UserLoginRequestDTO;
import com.be.web.dto.request.UserRegistrationDTO;
import com.be.web.dto.response.UserLoginResponseDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "https://tlab29.com")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<UserLoginResponseDTO> login(@Valid @RequestBody UserLoginRequestDTO loginRequest) {
        UserLoginResponseDTO response = authService.authenticate(loginRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<UserLoginResponseDTO> register(@Valid @RequestBody UserRegistrationDTO registrationDTO) {
        UserLoginResponseDTO response = authService.register(registrationDTO);
        return ResponseEntity.ok(response);
    }
}
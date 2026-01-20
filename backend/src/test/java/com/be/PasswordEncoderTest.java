package com.be;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class PasswordEncoderTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void testPasswordMatches() {
        String rawPassword = "Test1234";
        String hashedPassword = passwordEncoder.encode(rawPassword); // имитация пароля из БД

        // check for match
        assertTrue(passwordEncoder.matches(rawPassword, hashedPassword), "Password should match");
        // check for mismatch
        assertFalse(passwordEncoder.matches("WrongPassword", hashedPassword), "Password should not match");
    }
}
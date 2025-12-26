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
        String rawPassword = "Test1234"; // пароль, который вводит пользователь
        String hashedPassword = passwordEncoder.encode(rawPassword); // имитация пароля из БД

        // проверка совпадения
        assertTrue(passwordEncoder.matches(rawPassword, hashedPassword), "Пароль должен совпадать");

        // проверка несовпадения
        assertFalse(passwordEncoder.matches("WrongPassword", hashedPassword), "Пароль не должен совпадать");
    }
}
package com.be;

import com.be.domain.entity.User;
import com.be.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class PasswordMatchesDbTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void testPasswordMatchesDb() {
        // Ищем конкретного пользователя по email
        User user = userRepository.findByEmail("hudoshin@ukr.net")
                .orElseThrow(() -> new RuntimeException("User not found"));

        String enteredPassword = "Igibu.77777"; // пароль, который вводится при логине

        assertTrue(passwordEncoder.matches(enteredPassword, user.getPassword()),
                "Пароль должен совпадать с хешем из БД");
    }
}
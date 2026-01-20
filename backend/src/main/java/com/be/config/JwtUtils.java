package com.be.config;

import com.be.domain.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Objects;

@Component
public class JwtUtils {
    private static final Logger log = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${app.jwt.secret}")
    private String secret;

    // 1 day in ms
    private final long expiration = 86_400_000L;

    private Key signingKey;

    @PostConstruct
    public void init() {
        if (secret == null || secret.isBlank()) {
            log.error("JWT_SECRET is not set. Set environment variable JWT_SECRET (min 32 bytes).");
            throw new IllegalStateException("JWT secret is not configured");
        }

        // ensure key length (HS256 needs 256-bit = 32 bytes). Accept base64 or raw string.
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            log.error("JWT_SECRET is too short ({} bytes). It must be at least 32 bytes for HS256.", keyBytes.length);
            throw new IllegalStateException("JWT secret is too short (min 32 bytes)");
        }

        try {
            signingKey = Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception ex) {
            log.error("Failed to create HMAC key from JWT_SECRET: {}", ex.getMessage(), ex);
            throw new IllegalStateException("Invalid JWT secret", ex);
        }

        log.info("JwtUtils initialized, token expiration {}s", expiration / 1000);
    }

    public String generateToken(User user) {
        try {
            Objects.requireNonNull(user, "user must not be null");

            String email = user.getEmail();
            if (email == null || email.isBlank()) {
                log.warn("Generating JWT for user with null/empty email; using 'unknown' as subject");
                email = "unknown";
            }

            Long id = user.getId();

            String role;
            if (user.getRole() != null) {
                role = user.getRole().name();
            } else {
                log.warn("User id={} has null role, falling back to USER", id);
                role = "USER";
            }

            Date now = new Date();
            Date exp = new Date(System.currentTimeMillis() + expiration);

            return Jwts.builder()
                    .setSubject(email)
                    .claim("id", id)
                    .claim("role", role)
                    .setIssuedAt(now)
                    .setExpiration(exp)
                    .signWith(signingKey, SignatureAlgorithm.HS256)
                    .compact();
        } catch (RuntimeException ex) {
            log.error("Failed to generate JWT for user (id={}): {}",
                    user != null ? user.getId() : "null", ex.getMessage(), ex);
            throw ex; // пробрасываем, чтобы upstream видел понятную ошибку
        }
    }

    public long getExpirationTime() {
        return expiration / 1000; // seconds
    }
}
package com.be.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@ConfigurationProperties(prefix = "cors")
@Data
public class CorsProperties {

    private List<String> allowedOrigins = Arrays.asList(
            "https://tlab29.com",
            "http://tlab29.com",
            "https://api.tlab29.com",
            "http://api.tlab29.com",
            "http://localhost:3000",
            "http://localhost:8080",
            "https://localhost:3000",
            "https://localhost:8080"
    );

    /**
     * Опционально: паттерны для поддоменов, удобно для тестов/preview.
     * Если не нужен — можно оставить пустым.
     */
    private List<String> allowedOriginPatterns = Arrays.asList(
            "https://tlab29.com",
            "https://*.tlab29.com",
            "https://api.tlab29.com"
    );

    private boolean allowCredentials = true;

    private Long maxAge = 3600L;

    @PostConstruct
    public void normalize() {
        if (allowedOrigins != null) {
            allowedOrigins = allowedOrigins.stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
        if (allowedOriginPatterns != null) {
            allowedOriginPatterns = allowedOriginPatterns.stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
    }
}
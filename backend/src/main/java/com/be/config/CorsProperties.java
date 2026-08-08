package com.be.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "cors")
@Data
public class CorsProperties {

    // LR-033 — prod-only default, deliberately: this is the fallback if
    // application.properties's own cors.allowed-origins were ever absent,
    // not just the day-to-day value (that comes from the properties file,
    // which itself defaults to the same prod origins via
    // ${CORS_ALLOWED_ORIGINS:...}). Never add localhost here — that's what
    // the env var override is for.
    private List<String> allowedOrigins = List.of(
            "https://tlab29.com",
            "http://tlab29.com",
            "https://www.tlab29.com",
            "https://api.tlab29.com");

    private boolean allowCredentials = true;
    private Long maxAge = 3600L;
}
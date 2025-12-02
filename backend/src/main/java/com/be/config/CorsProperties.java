package com.be.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

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
            "http://localhost:8080"
    );

    private boolean allowCredentials = true;

    private Long maxAge = 3600L;
}
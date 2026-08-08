package com.be.config;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LR-033 — application.properties (the file actually deployed to prod)
 * used to hardcode localhost dev origins alongside allow-credentials=true.
 * Loads the REAL application.properties from the classpath (via
 * {@code @SpringBootTest}, not a duplicated literal here) to guard against
 * that regressing.
 */
class CorsPropertiesTest {

    @Nested
    @SpringBootTest(classes = CorsProperties.class)
    @EnableConfigurationProperties(CorsProperties.class)
    class DefaultConfiguration {

        @Autowired
        private CorsProperties corsProperties;

        @Test
        void doesNotIncludeLocalhostOrigins() {
            assertThat(corsProperties.getAllowedOrigins())
                    .noneMatch(origin -> origin.contains("localhost"));
        }

        @Test
        void includesTheRealProdOrigins() {
            assertThat(corsProperties.getAllowedOrigins())
                    .contains("https://tlab29.com", "https://api.tlab29.com");
        }
    }

    @Nested
    @SpringBootTest(classes = CorsProperties.class,
            properties = "CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:8080")
    @EnableConfigurationProperties(CorsProperties.class)
    class WithLocalDevOverride {

        @Autowired
        private CorsProperties corsProperties;

        @Test
        void envVarOverrideReplacesTheDefaultEntirely() {
            assertThat(corsProperties.getAllowedOrigins())
                    .containsExactly("http://localhost:3000", "http://localhost:8080");
        }
    }
}

package com.be.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.support.ResourcePropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LR-034 — application.properties (the file actually deployed to prod)
 * used to hardcode DEBUG logging for Spring Security/Web/our own package
 * permanently, not as a diagnostic opt-in. Resolves the real file's
 * ${ENV_VAR:default} placeholders through a real Environment (not a
 * duplicated literal here) rather than just grepping the raw text.
 */
class LoggingLevelsTest {

    @Test
    void defaultLevels_areNotDebug() throws Exception {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addLast(
                new ResourcePropertySource("classpath:application.properties"));

        assertThat(environment.getProperty("logging.level.org.springframework.security")).isEqualTo("WARN");
        assertThat(environment.getProperty("logging.level.org.springframework.web")).isEqualTo("WARN");
        assertThat(environment.getProperty("logging.level.com.be")).isEqualTo("INFO");
    }

    @Test
    void envVarOverride_actuallyWorks() throws Exception {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addLast(
                new ResourcePropertySource("classpath:application.properties"));
        environment.getPropertySources().addFirst(
                new MapPropertySource("test-override", Map.of("LOG_LEVEL_APP", "DEBUG")));

        assertThat(environment.getProperty("logging.level.com.be")).isEqualTo("DEBUG");
        // Untouched overrides still resolve to their own defaults.
        assertThat(environment.getProperty("logging.level.org.springframework.security")).isEqualTo("WARN");
    }
}

package com.be.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DatabaseFixConfig {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void dropNotNullConstraints() {
        log.info("Checking and dropping NOT NULL constraints on workshop_groups table...");
        try {
            // Drop NOT NULL for activity_id
            jdbcTemplate.execute("ALTER TABLE workshop_groups ALTER COLUMN activity_id DROP NOT NULL");
            log.info("Dropped NOT NULL constraint for activity_id");
            
            // Drop NOT NULL for age_group_id
            jdbcTemplate.execute("ALTER TABLE workshop_groups ALTER COLUMN age_group_id DROP NOT NULL");
            log.info("Dropped NOT NULL constraint for age_group_id");

            // Drop NOT NULL for language_id
            jdbcTemplate.execute("ALTER TABLE workshop_groups ALTER COLUMN language_id DROP NOT NULL");
            log.info("Dropped NOT NULL constraint for language_id");

            // Drop NOT NULL for teacher_id
            jdbcTemplate.execute("ALTER TABLE workshop_groups ALTER COLUMN teacher_id DROP NOT NULL");
            log.info("Dropped NOT NULL constraint for teacher_id");
            
        } catch (Exception e) {
            log.warn("Could not drop some NOT NULL constraints (they might not exist or table not created yet): {}", e.getMessage());
        }
    }
}

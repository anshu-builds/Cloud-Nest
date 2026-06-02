package com.cloudnest.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import jakarta.annotation.PostConstruct;

@Configuration
public class DatabaseMigrationConfig {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseMigrationConfig(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void migrateNullVersions() {
        try {
            // Fix BUG where @Version was added later, causing NullPointerException
            // "Cannot invoke java.lang.Long.longValue() because current is null" 
            // when saving existing entities.
            jdbcTemplate.execute("UPDATE folders SET version = 0 WHERE version IS NULL");
            jdbcTemplate.execute("UPDATE files SET version = 0 WHERE version IS NULL");
            System.out.println("✅ Database Migration: Successfully updated null versions to 0.");
        } catch (Exception e) {
            System.err.println("⚠️ Database Migration failed (this is normal on fresh DBs): " + e.getMessage());
        }
    }
}

package com.demo.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies safe committed configuration without requiring a database connection. */
class EnvironmentHardeningTest {

    private Properties load(String name) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(name)) {
            assertNotNull(input, "Missing configuration " + name);
            properties.load(input);
        }
        return properties;
    }

    @Test
    void sharedDefaultsDisableUnsafeDevelopmentFeatures() throws Exception {
        Properties properties = load("application.properties");
        assertEquals("false", properties.getProperty("spring.h2.console.enabled"));
        assertEquals("false", properties.getProperty("spring.jpa.show-sql"));
        assertEquals("true", properties.getProperty("spring.thymeleaf.cache"));
        assertNull(properties.getProperty("spring.security.user.password"));
        assertNull(properties.getProperty("spring.security.user.name"));
    }

    @Test
    void developmentConveniencesRequireExplicitProfile() throws Exception {
        Properties properties = load("application-dev.properties");
        assertEquals("true", properties.getProperty("spring.h2.console.enabled"));
        assertEquals("true", properties.getProperty("spring.jpa.show-sql"));
    }

    @Test
    void postgresOverridesDeveloperSettings() throws Exception {
        Properties properties = load("application-postgres.properties");
        assertEquals("false", properties.getProperty("spring.h2.console.enabled"));
        assertEquals("false", properties.getProperty("spring.jpa.show-sql"));
        assertEquals("true", properties.getProperty("spring.thymeleaf.cache"));
        assertEquals("validate", properties.getProperty("spring.jpa.hibernate.ddl-auto"));
        assertEquals("true", properties.getProperty("spring.flyway.enabled"));
    }
}

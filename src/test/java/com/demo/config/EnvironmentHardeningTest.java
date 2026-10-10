package com.demo.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies safe committed configuration without requiring a database connection. */
class EnvironmentHardeningTest {

    private Properties load(String name) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(Path.of("src", "main", "resources", name))) {
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
    @Test
    void demoCredentialsAreUnpredictableWithoutExplicitConfiguration() {
        DataInitializer initializer = mock(DataInitializer.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(initializer, "environment", new MockEnvironment());
        String first = initializer.demoPassword();
        String second = initializer.demoPassword();
        assertNotNull(first);
        assertTrue(first.length() >= 32);
        assertNotEquals(first, second);
    }

    @Test
    void demoCredentialsCanBeConfiguredOnlyThroughExternalProperty() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("onlyfilm.demo.password", "valor-local-de-prueba");
        DataInitializer initializer = mock(DataInitializer.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(initializer, "environment", environment);
        assertEquals("valor-local-de-prueba", initializer.demoPassword());
    }

}

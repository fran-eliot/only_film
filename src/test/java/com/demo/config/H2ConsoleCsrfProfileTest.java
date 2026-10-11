package com.demo.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Protección H2: la excepción CSRF es opt-in y no puede habilitarse en PostgreSQL. */
class H2ConsoleCsrfProfileTest {

    @Test
    void defaultProfileDoesNotAllowH2CsrfException() {
        assertFalse(SecurityConfig.isLocalH2ConsoleAllowed(new MockEnvironment()));
    }

    @Test
    void explicitDevelopmentProfileAllowsH2CsrfException() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");
        assertFalse(SecurityConfig.isLocalH2ConsoleAllowed(environment));
        environment.withProperty("spring.h2.console.enabled", "true")
                .withProperty("server.address", "127.0.0.1");
        assertTrue(SecurityConfig.isLocalH2ConsoleAllowed(environment));
    }

    @Test
    void publicBindingRejectsH2CsrfExceptionEvenWithDevelopmentProfile() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.h2.console.enabled", "true")
                .withProperty("server.address", "0.0.0.0");
        environment.setActiveProfiles("dev");
        assertFalse(SecurityConfig.isLocalH2ConsoleAllowed(environment));
    }

    @Test
    void consoleDisabledRejectsH2CsrfException() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.h2.console.enabled", "false")
                .withProperty("server.address", "127.0.0.1");
        environment.setActiveProfiles("dev");
        assertFalse(SecurityConfig.isLocalH2ConsoleAllowed(environment));
    }

    @Test
    void postgresProfileBlocksH2CsrfException() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("postgres");
        assertFalse(SecurityConfig.isLocalH2ConsoleAllowed(environment));
    }

    @Test
    void testAndDemoProfilesAloneDoNotAllowH2CsrfException() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("test");
        assertFalse(SecurityConfig.isLocalH2ConsoleAllowed(environment));
        environment.setActiveProfiles("demo");
        assertFalse(SecurityConfig.isLocalH2ConsoleAllowed(environment));
    }

    @Test
    void productionProfileBlocksH2CsrfException() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        assertFalse(SecurityConfig.isLocalH2ConsoleAllowed(environment));
    }

    @Test
    void productionWinsOverDevelopmentProfile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev", "prod");
        assertFalse(SecurityConfig.isLocalH2ConsoleAllowed(environment));
    }

    @Test
    void postgresWinsOverDevelopmentProfile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev", "postgres");
        assertFalse(SecurityConfig.isLocalH2ConsoleAllowed(environment));
    }
}

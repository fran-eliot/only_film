package com.demo.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityConfigH2ConsoleTest {

    private static MockEnvironment environment(String... profiles) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profiles);
        environment.setProperty("spring.h2.console.enabled", "true");
        environment.setProperty("server.address", "127.0.0.1");
        return environment;
    }

    @Test
    void permitsH2ConsoleOnlyForExplicitLocalDev() {
        assertTrue(SecurityConfig.isLocalH2ConsoleAllowed(environment("dev")));
    }

    @Test
    void rejectsH2ConsoleOutsideDev() {
        assertFalse(SecurityConfig.isLocalH2ConsoleAllowed(environment()));
        assertFalse(SecurityConfig.isLocalH2ConsoleAllowed(environment("prod")));
        assertFalse(SecurityConfig.isLocalH2ConsoleAllowed(environment("postgres")));
        assertFalse(SecurityConfig.isLocalH2ConsoleAllowed(environment("staging")));
    }

    @Test
    void rejectsDevCombinedWithAnyOtherProfile() {
        assertFalse(SecurityConfig.isLocalH2ConsoleAllowed(environment("dev", "prod")));
        assertFalse(SecurityConfig.isLocalH2ConsoleAllowed(environment("dev", "postgres")));
        assertFalse(SecurityConfig.isLocalH2ConsoleAllowed(environment("dev", "staging")));
    }

    @Test
    void rejectsDisabledH2Console() {
        MockEnvironment environment = environment("dev");
        environment.setProperty("spring.h2.console.enabled", "false");
        assertFalse(SecurityConfig.isLocalH2ConsoleAllowed(environment));
    }

    @Test
    void rejectsNonLoopbackServerBinding() {
        MockEnvironment environment = environment("dev");
        environment.setProperty("server.address", "0.0.0.0");
        assertFalse(SecurityConfig.isLocalH2ConsoleAllowed(environment));
        environment.setProperty("server.address", "::1");
        assertFalse(SecurityConfig.isLocalH2ConsoleAllowed(environment));
    }
}

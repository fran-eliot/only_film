package com.demo.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityConfigH2GateTest {

    private static MockEnvironment localDevEnvironment() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.h2.console.enabled", "true")
                .withProperty("server.address", "127.0.0.1");
        environment.setActiveProfiles("dev");
        return environment;
    }

    @Test
    void localDevWithEnabledConsoleAllowsH2Exception() {
        assertTrue(SecurityConfig.isLocalH2ConsoleAllowed(localDevEnvironment()));
    }

    @Test
    void disabledConsoleDoesNotAllowH2Exception() {
        MockEnvironment environment = localDevEnvironment()
                .withProperty("spring.h2.console.enabled", "false");
        assertFalse(SecurityConfig.isLocalH2ConsoleAllowed(environment));
    }

    @Test
    void nonLoopbackAddressDoesNotAllowH2Exception() {
        MockEnvironment environment = localDevEnvironment()
                .withProperty("server.address", "0.0.0.0");
        assertFalse(SecurityConfig.isLocalH2ConsoleAllowed(environment));
    }

    @Test
    void missingAddressDoesNotAllowH2Exception() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.h2.console.enabled", "true");
        environment.setActiveProfiles("dev");
        assertFalse(SecurityConfig.isLocalH2ConsoleAllowed(environment));
    }

    @Test
    void standardAndMixedProfilesDoNotAllowH2Exception() {
        for (String[] profiles : new String[][] {
                {}, {"test"}, {"prod"}, {"dev", "test"}, {"dev", "prod"}
        }) {
            MockEnvironment environment = localDevEnvironment();
            environment.setActiveProfiles(profiles);
            assertFalse(SecurityConfig.isLocalH2ConsoleAllowed(environment));
        }
    }
}

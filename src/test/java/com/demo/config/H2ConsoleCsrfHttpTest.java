package com.demo.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Checks the actual Spring Security filter chain with the local H2 dev opt-in. */
@SpringBootTest(properties = {
        "spring.h2.console.enabled=true",
        "server.address=127.0.0.1"
})
@ActiveProfiles("dev")
@AutoConfigureMockMvc
class H2ConsoleCsrfHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void h2ConsolePostIsExcludedFromCsrfOnlyWhenExplicitlyEnabled() throws Exception {
        mockMvc.perform(post("/h2-console/login.do").with(user("dev-admin").roles("ADMIN")))
                .andExpect(result -> assertNotEquals(403, result.getResponse().getStatus(),
                        "Local H2 console requests should not be rejected by the CSRF filter"));
    }

    @Test
    void localH2OptInUsesSameOriginFrameHeaderOnApplicationPages() throws Exception {
        mockMvc.perform(get("/movies"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Frame-Options", "SAMEORIGIN"));
    }

    @Test
    void normalMutationStillRequiresCsrfWithH2ConsoleEnabled() throws Exception {
        mockMvc.perform(post("/movies/deactivate/1").with(user("dev-admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }
}

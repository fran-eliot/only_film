package com.demo.controller;

import com.demo.model.User;
import com.demo.model.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * State mutations must never be available through GET.
 * Spring Security must reject unsafe requests without a CSRF token and
 * enforce admin authorization for POST /sessions/deactivate as well.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MutationCsrfSecurityTest {

    @Autowired MockMvc mockMvc;

    private User admin() {
        return User.builder().id(9001L).username("admin-csrf")
                .role(Role.ROLE_ADMIN).active(true).password("unused").build();
    }

    private User member() {
        return User.builder().id(9002L).username("member-csrf")
                .role(Role.ROLE_USER).active(true).password("unused").build();
    }

    @Test
    void getNeverMutatesExistingResources() throws Exception {
        for (String path : new String[] {
                "/movies/deactivate/1", "/salas/deactivate/1",
                "/sessions/deactivate/1", "/reviews/delete/1",
                "/admin/users/deactivate/1", "/admin/users/activate/1"
        }) {
            mockMvc.perform(get(path).with(user(admin())))
                    .andExpect(status().isMethodNotAllowed());
        }
    }

    @Test
    void postWithoutCsrfIsForbiddenForAllStateMutations() throws Exception {
        for (String path : new String[] {
                "/movies/deactivate/1", "/salas/deactivate/1",
                "/sessions/deactivate/1", "/reviews/delete/1",
                "/admin/users/deactivate/1", "/admin/users/activate/1"
        }) {
            mockMvc.perform(post(path).with(user(admin())))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void nonAdminCannotDeactivateSessionsEvenWithCsrf() throws Exception {
        mockMvc.perform(post("/sessions/deactivate/1")
                        .with(user(member())).with(csrf()))
                .andExpect(status().isForbidden());
    }
}

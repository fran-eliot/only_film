package com.demo.controller;

import com.demo.model.User;
import com.demo.model.enums.Role;
import com.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc //Aquí no ponemos el (addFilters = false) para que la seguridad funcione
public class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User adminUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        adminUser = userRepository.save(User.builder()
                .username("admintest")
                .email("admintest@gmail.com")
                .firstName("Admin")
                .lastName("Test")
                .password(passwordEncoder.encode("admin"))
                .role(Role.ROLE_ADMIN)
                .active(true)
                .build());
    }

    @Test
    void anonymousUserCanAccessMovies() throws Exception {
        // Simula un usuario anónimo entrando a la lista de películas
        mockMvc.perform(get("/movies"))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousUserIsRedirectedFromNewMovieForm() throws Exception {
        // Simula un usuario anónimo intentando crear una película
        mockMvc.perform(get("/movies/new"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void adminUserCanAccessNewMovieForm() throws Exception {
        mockMvc.perform(get("/movies/new")
                        .with(user(adminUser)))
                .andExpect(status().isOk());
    }

    @Test
    void defaultTestProfileKeepsDenyFrameProtection() throws Exception {
        mockMvc.perform(get("/movies"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    @Test
    void h2ConsoleEndpointDoesNotBypassCsrfInTestOrDefaultProfile() throws Exception {
        mockMvc.perform(post("/h2-console/login.do").with(user(adminUser)))
                .andExpect(status().isForbidden());
    }

}

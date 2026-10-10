package com.demo.controller;

import com.demo.model.User;
import com.demo.model.enums.Role;
import com.demo.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Tests para UserController")
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User admin;
    private User normalUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        admin = userRepository.save(User.builder()
                .username("admin")
                .email("admin@onlyfilm.com")
                .firstName("Admin")
                .lastName("OnlyFilm")
                .password(passwordEncoder.encode("admin123"))
                .role(Role.ROLE_ADMIN)
                .active(true)
                .build());

        normalUser = userRepository.save(User.builder()
                .username("fran")
                .email("fran@onlyfilm.com")
                .firstName("Fran")
                .lastName("Ramírez")
                .password(passwordEncoder.encode("user123"))
                .role(Role.ROLE_USER)
                .active(true)
                .build());
    }

    @Test
    @DisplayName("GET /admin/users muestra el listado de usuarios")
    void listUsers_returnsUserListView() throws Exception {
        mockMvc.perform(get("/admin/users").with(user(admin)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("users/user-list"))
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attribute("users", hasSize(2)));
    }

    @Test
    @DisplayName("GET /admin/users/{id} muestra el detalle de un usuario")
    void detail_existingUser_returnsUserDetailView() throws Exception {
        mockMvc.perform(get("/admin/users/" + normalUser.getId()).with(user(admin)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("users/user-detail"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("userStats"))
                .andExpect(model().attribute("user", hasProperty("id", is(normalUser.getId()))))
                .andExpect(model().attribute("user", hasProperty("username", is("fran"))))
                .andExpect(model().attribute("user", hasProperty("email", is("fran@onlyfilm.com"))));
    }

    @Test
    @DisplayName("GET /admin/users/new muestra formulario de creación")
    void newUser_returnsUserFormView() throws Exception {
        mockMvc.perform(get("/admin/users/new").with(user(admin)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("users/user-form"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("roles"))
                .andExpect(model().attribute("edit", false));
    }

    @Test
    @DisplayName("GET /admin/users/edit/{id} muestra formulario de edición")
    void editUser_existingUser_returnsUserFormView() throws Exception {
        mockMvc.perform(get("/admin/users/edit/" + normalUser.getId()).with(user(admin)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("users/user-form"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("roles"))
                .andExpect(model().attribute("edit", true))
                .andExpect(model().attribute("user", hasProperty("id", is(normalUser.getId()))))
                .andExpect(model().attribute("user", hasProperty("password", nullValue())));
    }

    @Test
    @DisplayName("POST /admin/users crea un usuario nuevo")
    void save_newUser_createsUserAndRedirects() throws Exception {
        long before = userRepository.count();

        mockMvc.perform(multipart("/admin/users")
                        .file("imageFile", new byte[0])
                        .param("username", "barbara")
                        .param("email", "barbara@onlyfilm.com")
                        .param("firstName", "Barbara")
                        .param("lastName", "Urbano")
                        .param("password", "password123")
                        .param("role", Role.ROLE_USER.name())
                        .param("active", "true")
                        .with(user(admin))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attribute("message", "Usuario creado correctamente"));

        long after = userRepository.count();

        assertEquals(before + 1, after);
        assertTrue(userRepository.existsByUsername("barbara"));
        assertTrue(userRepository.existsByEmail("barbara@onlyfilm.com"));
    }

    @Test
    @DisplayName("POST /admin/users actualiza un usuario existente")
    void save_existingUser_updatesUserAndRedirects() throws Exception {
        mockMvc.perform(multipart("/admin/users")
                        .file("imageFile", new byte[0])
                        .param("id", normalUser.getId().toString())
                        .param("username", "fran_updated")
                        .param("email", "fran.updated@onlyfilm.com")
                        .param("firstName", "Fran")
                        .param("lastName", "Ramírez Martín")
                        .param("role", Role.ROLE_USER.name())
                        .param("active", "true")
                        .with(user(admin))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attribute("message", "Usuario actualizado correctamente"));

        User updated = userRepository.findById(normalUser.getId()).orElseThrow();

        assertEquals("fran_updated", updated.getUsername());
        assertEquals("fran.updated@onlyfilm.com", updated.getEmail());
        assertEquals(Role.ROLE_USER, updated.getRole());
        assertTrue(updated.getActive());
    }

    @Test
    @DisplayName("POST /admin/users/deactivate/{id} desactiva un usuario")
    void deactivate_existingUser_setsActiveFalse() throws Exception {
        assertTrue(normalUser.getActive());

        mockMvc.perform(post("/admin/users/deactivate/" + normalUser.getId()).with(user(admin)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attribute("message", "Usuario desactivado correctamente"));

        User deactivated = userRepository.findById(normalUser.getId()).orElseThrow();

        assertFalse(deactivated.getActive());
    }

    @Test
    @DisplayName("POST /admin/users/activate/{id} activa un usuario")
    void activate_existingUser_setsActiveTrue() throws Exception {
        normalUser.setActive(false);
        userRepository.save(normalUser);

        mockMvc.perform(post("/admin/users/activate/" + normalUser.getId()).with(user(admin)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attribute("message", "Usuario activado correctamente"));

        User activated = userRepository.findById(normalUser.getId()).orElseThrow();

        assertTrue(activated.getActive());
    }

    @Test
    @DisplayName("GET /profile muestra el perfil del usuario autenticado")
    void profile_authenticatedUser_returnsUserDetailView() throws Exception {
        mockMvc.perform(get("/profile").with(user(normalUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("users/user-detail"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("userStats"))
                .andExpect(model().attribute("user", hasProperty("id", is(normalUser.getId()))))
                .andExpect(model().attribute("user", hasProperty("username", is("fran"))));
    }

    @Test
    @DisplayName("POST /admin/users con username duplicado redirige al formulario de creación con error")
    void save_newUserWithDuplicatedUsername_redirectsWithError() throws Exception {
        long before = userRepository.count();

        mockMvc.perform(multipart("/admin/users")
                        .file("imageFile", new byte[0])
                        .param("username", "fran")
                        .param("email", "otro@onlyfilm.com")
                        .param("firstName", "Otro")
                        .param("lastName", "Usuario")
                        .param("password", "password123")
                        .param("role", Role.ROLE_USER.name())
                        .param("active", "true")
                        .with(user(admin))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/new"))
                .andExpect(flash().attributeExists("error"));

        long after = userRepository.count();

        assertEquals(before, after);
    }
}

package com.demo.controller;

import com.demo.dto.RegisterForm;
import com.demo.model.User;
import com.demo.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;

// @SpringBootTest
@WebMvcTest(controllers = AuthController.class) // carga solo este controller
@AutoConfigureMockMvc(addFilters = false) // sin activar Security
public class AuthControllerTest {

    @MockitoBean
    UserService userService;

    @Autowired
    MockMvc mockMvc;


    @Test
    void loginTest() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
        verifyNoInteractions(userService);
    }

    @Test
    void registerFormDoesNotStealInitialFocus() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("autofocus"))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("for=\"username\"")));
    }

    @Test
    void registerTest() throws Exception {
        when(userService.register(any(RegisterForm.class))).thenReturn(User.builder().username("elpepe").build());

        mockMvc.perform(post("/register")
                        .param("username", "elpepe")
                        .param("email", "elpepe@gmail.com")
                        .param("firstName","Pepe")
                        .param("lastName","Pepillo")
                        .param("password", "user")
                        .param("passwordConfirm", "user")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("message", "Cuenta creada correctamente, inicia sesión."));

        verify(userService).register(any(RegisterForm.class));
    }

}

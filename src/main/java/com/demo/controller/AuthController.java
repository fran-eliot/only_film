package com.demo.controller;

import com.demo.service.UserService;
import com.demo.dto.RegisterForm;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// controlador para iniciar sesion y/ registrarse crear User en db
@Controller
@AllArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("register")
    public String register(Model model) {
        model.addAttribute("user", new RegisterForm());
        return "auth/register";
    }

    @PostMapping("register")
    public String register(
            @Valid @ModelAttribute ("user") RegisterForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        //Validamos que las contraseñas coincidan y las conserva"
        if(form.getPassword() != null && !form.getPassword().equals(form.getPasswordConfirm())){
            bindingResult.rejectValue("passwordConfirm", "error.passwordConfirm", "Las contraseñas no coindicen");
        }

        // (Si hay errores de coincidencia), vuelve al formulario
        if (bindingResult.hasErrors()) {
            return "auth/register";   // NO redirect, para que se mantengan los errores
        }

        try {
            userService.register(form);
            redirectAttributes.addFlashAttribute("message", "Cuenta creada correctamente, inicia sesión.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            // Si el username/email ya existe lo mostramos como error general y mantenemos lo que habia puesto el usuario
            model.addAttribute("error", e.getMessage());
            return "auth/register";
        }
    }

    @GetMapping("login")
    public String login(){
        return "auth/login";
    }
}

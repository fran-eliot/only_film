package com.demo.controller;

import com.demo.model.User;
import com.demo.model.enums.Role;
import com.demo.service.FileService;
import com.demo.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@AllArgsConstructor
@Controller
public class UserController {

    private static final String ATTR_USER = "user";
    private static final String ATTR_USERS = "users";
    private static final String ATTR_USER_STATS = "userStats";
    private static final String ATTR_ROLES = "roles";
    private static final String ATTR_EDIT = "edit";
    private static final String ATTR_MESSAGE = "message";
    private static final String ATTR_ERROR = "error";

    private static final String VIEW_USER_LIST = "users/user-list";
    private static final String VIEW_USER_DETAIL = "users/user-detail";
    private static final String VIEW_USER_FORM = "users/user-form";
    private static final String VIEW_PROFILE_FORM = "users/profile-form";

    private static final String REDIRECT_USERS = "redirect:/admin/users";
    private static final String REDIRECT_USER_NEW = "redirect:/admin/users/new";
    private static final String REDIRECT_USER_EDIT = "redirect:/admin/users/edit/";
    private static final String REDIRECT_PROFILE = "redirect:/profile";

    private static final String MSG_USER_CREATED = "Usuario creado correctamente";
    private static final String MSG_USER_UPDATED = "Usuario actualizado correctamente";
    private static final String MSG_USER_DEACTIVATED = "Usuario desactivado correctamente";
    private static final String MSG_USER_ACTIVATED = "Usuario activado correctamente";
    private static final String MSG_PROFILE_UPDATED = "Usuario actualizado";
    private static final String MSG_NO_PERMISSION = "No tienes permisos";
    private static final String MSG_USER_CREATE_ERROR = "Error al crear el usuario";

    private final UserService userService;
    private final FileService fileService;

    @GetMapping("admin/users")
    public String list(Model model) {
        model.addAttribute(ATTR_USERS, userService.findAll());
        return VIEW_USER_LIST;
    }

    // Para acceder a un usuario en particular, user-detail
    @GetMapping("admin/users/{id}")
    public String detail(Model model, @PathVariable Long id) {
        model.addAttribute(ATTR_USER, userService.findById(id));
        model.addAttribute(ATTR_USER_STATS, userService.findStatsById(id));
        return VIEW_USER_DETAIL;
    }

    // GetMapping admin/users/new
    @GetMapping("admin/users/new")
    public String newUser(Model model) {
        model.addAttribute(ATTR_USER, new User());
        model.addAttribute(ATTR_ROLES, Role.values());
        model.addAttribute(ATTR_EDIT, false);

        return VIEW_USER_FORM;
    }

    // GetMapping admin/users/edit/{id}
    @GetMapping("admin/users/edit/{id}")
    public String editUser(Model model, @PathVariable Long id) {
        User user = userService.findById(id);
        user.setPassword(null); // Se setea en null para no exponerla

        model.addAttribute(ATTR_USER, user);
        model.addAttribute(ATTR_ROLES, Role.values());
        model.addAttribute(ATTR_EDIT, true);

        return VIEW_USER_FORM;
    }

    // PostMapping admin/users
    @PostMapping("admin/users")
    public String save(
            @ModelAttribute User user,
            BindingResult bindingResult,
            @AuthenticationPrincipal User currentUser,
            RedirectAttributes redirectAttributes,
            @RequestParam("imageFile") MultipartFile imageFile,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute(ATTR_ROLES, Role.values());
            model.addAttribute(ATTR_EDIT, user.getId() != null);
            return VIEW_USER_FORM; // vuelve al form SIN guardar.
        }

        // Los logs indican fecha, PID, clase y línea donde ocurre el evento.
        // Requiere la anotación @Slf4j.
        log.info("Guardando user {}", user.getUsername());

        String imageUrl;
        try {
            imageUrl = fileService.store(imageFile);
        } catch (IllegalArgumentException invalidImage) {
            redirectAttributes.addFlashAttribute(ATTR_ERROR, invalidImage.getMessage());
            return user.getId() == null ? REDIRECT_USER_NEW : REDIRECT_USER_EDIT + user.getId();
        }

        // Never accept a hidden imageUrl field as a trusted upload path.
        user.setImageUrl(imageUrl);

        try {
            if (user.getId() == null) {
                user = userService.create(user);
                redirectAttributes.addFlashAttribute(ATTR_MESSAGE, MSG_USER_CREATED);
                log.info("{} {}", MSG_USER_CREATED, user);
            } else {
                user = userService.updateAdmin(user, currentUser.getId(), imageUrl);
                redirectAttributes.addFlashAttribute(ATTR_MESSAGE, MSG_USER_UPDATED);
                log.info("{} {}", MSG_USER_UPDATED, user);
            }

            // Tanto para CREAR como ACTUALIZAR redirige al listado de usuarios.
            return REDIRECT_USERS;
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ATTR_ERROR, MSG_USER_CREATE_ERROR);
            log.error("{} {}", MSG_USER_CREATE_ERROR, e.getMessage());

            return user.getId() == null
                    ? REDIRECT_USER_NEW
                    : REDIRECT_USER_EDIT + user.getId();
        }
    }

    @PostMapping("admin/users/deactivate/{id}")
    public String deactivate(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser,
            RedirectAttributes redirectAttributes
    ) {
        try {
            userService.deactivate(id, currentUser.getId());
            redirectAttributes.addFlashAttribute(ATTR_MESSAGE, MSG_USER_DEACTIVATED);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ATTR_ERROR, e.getMessage());
        }

        return REDIRECT_USERS;
    }

    @PostMapping("admin/users/activate/{id}")
    public String activate(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            userService.activate(id);
            redirectAttributes.addFlashAttribute(ATTR_MESSAGE, MSG_USER_ACTIVATED);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(ATTR_ERROR, e.getMessage());
        }

        return REDIRECT_USERS;
    }

    @GetMapping("profile")
    public String profile(Model model, @AuthenticationPrincipal User user) {
        model.addAttribute(ATTR_USER, userService.findById(user.getId()));
        model.addAttribute(ATTR_USER_STATS, userService.findStatsById(user.getId()));
        return VIEW_USER_DETAIL;
    }

    @GetMapping("profile/edit")
    public String editProfile(Model model, @AuthenticationPrincipal User user) {
        User saved = userService.findById(user.getId());
        saved.setPassword(null); // Evita exponer la contraseña en el formulario

        model.addAttribute(ATTR_USER, saved);
        return VIEW_PROFILE_FORM;
    }

    // PostMapping profile: permite al usuario autenticado actualizar su propio perfil.
    @PostMapping("profile")
    public String saveProfile(
            @ModelAttribute User userForm,
            RedirectAttributes redirectAttributes,
            @RequestParam("imageFile") MultipartFile imageFile,
            @AuthenticationPrincipal User authenticatedUser,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (authenticatedUser == null || authenticatedUser.getId() == null) {
            log.error("Error usuario {} intentando editar otro usuario {}", authenticatedUser, userForm);
            redirectAttributes.addFlashAttribute(ATTR_ERROR, MSG_NO_PERMISSION);
            return REDIRECT_PROFILE;
        }

        // The service resolves ID, role and active state from the database.\n
        // Imagen: si no se sube una nueva, mantiene la imagen actual.
        String imageUrl;
        try {
            imageUrl = fileService.store(imageFile);
        } catch (IllegalArgumentException invalidImage) {
            redirectAttributes.addFlashAttribute(ATTR_ERROR, invalidImage.getMessage());
            return "redirect:/profile/edit";
        }
        userForm.setImageUrl(imageUrl);

        User userUpdated = userService.updateProfile(userForm, authenticatedUser.getId(), imageUrl);

        // Refresca Spring Security para que el principal/navbar muestren los datos nuevos.
        Authentication newAuth = new UsernamePasswordAuthenticationToken(
                userUpdated,
                userUpdated.getPassword(),
                userUpdated.getAuthorities()
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(newAuth);

        SecurityContextHolder.setContext(context);

        new HttpSessionSecurityContextRepository().saveContext(context, request, response);

        redirectAttributes.addFlashAttribute(ATTR_MESSAGE, MSG_PROFILE_UPDATED);
        return REDIRECT_PROFILE;
    }
}

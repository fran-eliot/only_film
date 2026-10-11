package com.demo.service;

import com.demo.dto.UserStatsDTO;
import com.demo.model.User;
import com.demo.model.enums.BuyStatus;
import com.demo.model.enums.Role;
import com.demo.repository.ReviewRepository;
import com.demo.repository.TicketRepository;
import com.demo.repository.UserRepository;
import com.demo.dto.RegisterForm;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ReviewRepository reviewRepository;
    private final TicketRepository ticketRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("user not found with username: " + username) );
    }

    public User register(RegisterForm form) {

        if (userRepository.existsByUsername(form.getUsername()))
            throw new IllegalArgumentException("username ya existe, elige otro username");

        if (userRepository.existsByEmail(form.getEmail()))
            throw new IllegalArgumentException("email ya existe, elige otro email");

        if (! form.getPassword().equals(form.getPasswordConfirm()))
            throw new IllegalArgumentException("Las contraseñas no coinciden");


        User user = new User();
        user.setUsername(form.getUsername());
        user.setEmail(form.getEmail());
        user.setRole(Role.ROLE_USER);
        user.setFirstName(form.getFirstName());
        user.setLastName(form.getLastName());
        user.setActive(true);
        String encodedPassword = passwordEncoder.encode(form.getPassword());
        user.setPassword(encodedPassword);
        return userRepository.save(user);
    }
    //Utilizamos el Service para llamar desde aqui el repository que antes lo haciamos desde el controller
    public List<User> findAll(){
        return userRepository.findAll();
    }

    //En el metodo anterior indicamos que ese metodo nos devuelve algo de TIPO lista User
    //En este metodo como es 1 usuario en particular, el metodo devuelve algo de TIPO user.
    //Es lo mismo que haciamos antes en el controller, pedirle al Repository que nos de el user por id y si no que nos de una excepcion
    public User findById(Long id){
        return userRepository.findById(id).orElseThrow(() -> new java.util.NoSuchElementException("Usuario no encontrado"));
    }

    public User create(User user){
        if (userRepository.existsByUsername(user.getUsername())){
            throw new IllegalArgumentException("Este username ya está ocupado");
        }
        if (userRepository.existsByEmail(user.getEmail())){
            throw new IllegalArgumentException("Este email ya está ocupado");
        }

        //Si usamos Spring
        if (!StringUtils.hasText(user.getPassword()))
            throw new IllegalArgumentException("Password no puede estar vacía");

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    /**
     * A regular user may only update their own profile. Role, active state, ID,
     * and ownership are resolved from the authenticated user in the database,
     * never from the submitted form.
     */
    public User updateProfile(User form, Long authenticatedUserId, String validatedImageUrl) {
        if (authenticatedUserId == null) {
            throw new org.springframework.security.access.AccessDeniedException("Debes iniciar sesión");
        }
        User existing = findById(authenticatedUserId);
        updateContactFields(existing, form);
        if (StringUtils.hasText(validatedImageUrl)) {
            existing.setImageUrl(validatedImageUrl);
        }
        updatePasswordIfPresent(existing, form.getPassword());
        return userRepository.save(existing);
    }

    /**
     * Administrative update with independent authorization and invariants.
     * The actor is reloaded from storage to avoid trusting submitted roles.
     */
    public User updateAdmin(User form, Long authenticatedAdminId, String validatedImageUrl) {
        if (authenticatedAdminId == null) {
            throw new org.springframework.security.access.AccessDeniedException("No tienes permisos");
        }
        User actor = findById(authenticatedAdminId);
        if (actor.getRole() != Role.ROLE_ADMIN || !Boolean.TRUE.equals(actor.getActive())) {
            throw new org.springframework.security.access.AccessDeniedException("No tienes permisos");
        }
        if (form.getId() == null) {
            throw new IllegalArgumentException("ID de usuario obligatorio");
        }
        User existing = findById(form.getId());
        // Check the ORIGINAL role before applying any mutations.
        if (existing.getId().equals(actor.getId()) &&
                (form.getRole() != Role.ROLE_ADMIN || !Boolean.TRUE.equals(form.getActive()))) {
            throw new IllegalArgumentException("Un administrador no puede desactivarse ni quitarse su rol");
        }
        if (form.getRole() == null || form.getActive() == null) {
            throw new IllegalArgumentException("Rol y estado son obligatorios");
        }
        updateContactFields(existing, form);
        existing.setRole(form.getRole());
        existing.setActive(form.getActive());
        if (StringUtils.hasText(validatedImageUrl)) {
            existing.setImageUrl(validatedImageUrl);
        }
        updatePasswordIfPresent(existing, form.getPassword());
        return userRepository.save(existing);
    }

    private void updateContactFields(User existing, User form) {
        userRepository.findByUsername(form.getUsername())
                .filter(other -> !other.getId().equals(existing.getId()))
                .ifPresent(other -> { throw new IllegalArgumentException("Este username ya existe"); });
        userRepository.findByEmail(form.getEmail())
                .filter(other -> !other.getId().equals(existing.getId()))
                .ifPresent(other -> { throw new IllegalArgumentException("El email de usuario ya existe"); });
        existing.setUsername(form.getUsername());
        existing.setEmail(form.getEmail());
    }

    private void updatePasswordIfPresent(User existing, String newPassword) {
        if (StringUtils.hasText(newPassword)) {
            existing.setPassword(passwordEncoder.encode(newPassword));
        }
    }

    public void deactivate(Long userId, Long currentUserId) {
        User user = findById(userId);
        if (user.getRole() == Role.ROLE_ADMIN) {
            throw new IllegalArgumentException("No se puede desactivar una cuenta de administrador");
        }
        if (currentUserId.equals(userId)) {
            throw new IllegalArgumentException("No puedes desactivar tu propia cuenta");
        }
        user.setActive(false);
        userRepository.save(user);
    }

    public void activate(Long userId) {
        User user = findById(userId);
        user.setActive(true);
        userRepository.save(user);
    }

    public UserStatsDTO findStatsById(Long id) {
        return new UserStatsDTO(
                reviewRepository.countByUser_Id(id),
                reviewRepository.findByUser_IdOrderByCreationDateDesc(id),
                ticketRepository.findByUser_IdAndStatus(id, BuyStatus.PAGADO)
        );
    }

}

package com.demo.service;

import com.demo.dto.RegisterForm;
import com.demo.dto.UserStatsDTO;
import com.demo.model.Review;
import com.demo.model.Ticket;
import com.demo.model.User;
import com.demo.model.enums.BuyStatus;
import com.demo.model.enums.Role;
import com.demo.repository.ReviewRepository;
import com.demo.repository.TicketRepository;
import com.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private UserService userService;

    private RegisterForm defaultForm;

    @BeforeEach
    public void setUp() {
        defaultForm = new RegisterForm();
        defaultForm.setUsername("john");
        defaultForm.setEmail("john@example.com");
        defaultForm.setPassword("secret123");
        defaultForm.setPasswordConfirm("secret123");
    }

    @Test
    @DisplayName("register: datos válidos -> guarda usuario con contraseña encriptada")
    public void register_withValidData_savesUserWithEncodedPassword() {
        // Arrange
        RegisterForm form = defaultForm;

        when(userRepository.existsByUsername(form.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(form.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(form.getPassword())).thenReturn("ENCODED_secret123");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User saved = userService.register(form);

        // Assert
        assertNotNull(saved);
        assertEquals("john", saved.getUsername());
        assertEquals("john@example.com", saved.getEmail());
        assertEquals(Role.ROLE_USER, saved.getRole());
        assertEquals("ENCODED_secret123", saved.getPassword());

        // Verify interactions
        verify(userRepository, times(1)).existsByUsername("john");
        verify(userRepository, times(1)).existsByEmail("john@example.com");
        verify(passwordEncoder, times(1)).encode("secret123");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(captor.capture());
        User passedToSave = captor.getValue();
        assertEquals("john", passedToSave.getUsername());
        assertEquals("ENCODED_secret123", passedToSave.getPassword());

        verifyNoMoreInteractions(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("register: username ya existente -> lanza IllegalArgumentException")
    public void register_withExistingUsername_throwsException() {
        // Arrange
        RegisterForm form = new RegisterForm();
        form.setUsername("exists");
        form.setEmail("exists@example.com");
        form.setPassword("pwd");
        form.setPasswordConfirm("pwd");

        when(userRepository.existsByUsername("exists")).thenReturn(true);

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.register(form));
        assertTrue(ex.getMessage().toLowerCase().contains("username") || ex.getMessage().toLowerCase().contains("ya existe"));

        verify(userRepository, times(1)).existsByUsername("exists");
        verify(userRepository, never()).existsByEmail(anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
        verifyNoMoreInteractions(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("register: email ya existente -> lanza IllegalArgumentException")
    public void register_withExistingEmail_throwsException() {
        // Arrange
        RegisterForm form = defaultForm;
        when(userRepository.existsByUsername(form.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(form.getEmail())).thenReturn(true);

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.register(form));
        assertTrue(ex.getMessage().toLowerCase().contains("email") || ex.getMessage().toLowerCase().contains("ya existe"));

        verify(userRepository, times(1)).existsByUsername(form.getUsername());
        verify(userRepository, times(1)).existsByEmail(form.getEmail());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
        verifyNoMoreInteractions(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("register: contraseñas no coinciden -> lanza IllegalArgumentException")
    public void register_withPasswordMismatch_throwsException() {
        // Arrange
        RegisterForm form = new RegisterForm();
        form.setUsername("mark");
        form.setEmail("mark@example.com");
        form.setPassword("a");
        form.setPasswordConfirm("b");

        when(userRepository.existsByUsername("mark")).thenReturn(false);
        when(userRepository.existsByEmail("mark@example.com")).thenReturn(false);

        // Act & Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.register(form));
        assertTrue(ex.getMessage().toLowerCase().contains("contrase") || ex.getMessage().toLowerCase().contains("no coinciden"));

        verify(userRepository, times(1)).existsByUsername("mark");
        verify(userRepository, times(1)).existsByEmail("mark@example.com");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
        verifyNoMoreInteractions(userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("loadUserByUsername: usuario existente -> devuelve UserDetails correcto")
    public void loadUserByUsername_existingUser_returnsUserDetails() {
        // Arrange
        User user = User.builder()
                .username("alice")
                .firstName("Alice")
                .email("alice@example.com")
                .password("encodedPwd")
                .role(Role.ROLE_USER)
                .build();

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        // Act
        UserDetails details = userService.loadUserByUsername("alice");

        // Assert
        assertNotNull(details);
        assertEquals("alice", details.getUsername());
        assertSame(user, details);

        // Verificar que tiene autoridad correcta
        assertFalse(details.getAuthorities().isEmpty());
        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a != null && a.getAuthority() != null &&
                        a.getAuthority().equals(Role.ROLE_USER.name())));

        verify(userRepository, times(1)).findByUsername("alice");
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("loadUserByUsername: usuario no existente -> lanza UsernameNotFoundException")
    public void loadUserByUsername_nonExisting_throwsUsernameNotFoundException() {
        // Arrange
        when(userRepository.findByUsername("nope")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UsernameNotFoundException.class, () -> userService.loadUserByUsername("nope"));

        verify(userRepository, times(1)).findByUsername("nope");
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("create: usuario válido -> encripta contraseña y guarda")
    void create_withValidUser_encodesPasswordAndSaves() {
        User user = User.builder()
                .username("newuser")
                .email("new@example.com")
                .password("plainPassword")
                .role(Role.ROLE_USER)
                .active(true)
                .build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plainPassword")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.create(user);

        assertEquals("encodedPassword", result.getPassword());
        verify(passwordEncoder).encode("plainPassword");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("create: password vacío -> lanza excepción")
    void create_withBlankPassword_throwsException() {
        User user = User.builder()
                .username("newuser")
                .email("new@example.com")
                .password(" ")
                .build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.create(user)
        );

        assertTrue(ex.getMessage().contains("Password"));
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("profile: ignora ID, rol, estado e imagen inyectados")
    void updateProfile_ignoresPrivilegedAndForgedFields() {
        User persisted = User.builder().id(1L).username("old")
                .email("old@example.com").password("encoded")
                .role(Role.ROLE_USER).active(true).imageUrl("/uploads/existing.png").build();
        User forged = User.builder().id(999L).username("updated")
                .email("updated@example.com").password("")
                .role(Role.ROLE_ADMIN).active(false)
                .imageUrl("/uploads/malicious.png").build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(persisted));
        when(userRepository.findByUsername("updated")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("updated@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // The service accepts a path only if it has been validated and passed by its caller.
        // Identity and permissions always come from the saved user.
        User updated = userService.updateProfile(forged, 1L, null);
        assertEquals(1L, updated.getId());
        assertEquals(Role.ROLE_USER, updated.getRole());
        assertTrue(updated.getActive());
        assertEquals("updated", updated.getUsername());
        assertEquals("encoded", updated.getPassword());
        assertEquals("/uploads/existing.png", updated.getImageUrl());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("admin: impide desactivación o degradación del propio administrador")
    void updateAdmin_preventsSelfLockout() {
        User admin = User.builder().id(1L).username("admin")
                .email("admin@example.com").password("encoded")
                .role(Role.ROLE_ADMIN).active(true).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        for (User requested : List.of(
                User.builder().id(1L).role(Role.ROLE_ADMIN).active(false).build(),
                User.builder().id(1L).role(Role.ROLE_USER).active(true).build(),
                User.builder().id(1L).role(Role.ROLE_USER).active(false).build()
        )) {
            assertThrows(IllegalArgumentException.class, () -> userService.updateAdmin(requested, 1L, null));
        }
        assertEquals(Role.ROLE_ADMIN, admin.getRole());
        assertTrue(admin.getActive());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("admin: un usuario normal no puede invocar actualización administrativa")
    void updateAdmin_rejectsNormalUser() {
        User actor = User.builder().id(4L).role(Role.ROLE_USER).active(true).build();
        when(userRepository.findById(4L)).thenReturn(Optional.of(actor));
        User request = User.builder().id(5L).role(Role.ROLE_ADMIN).active(true).build();
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> userService.updateAdmin(request, 4L, null));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("admin: puede editar otro usuario, conservando contraseña sin cambio")
    void updateAdmin_otherUserPreservesPassword() {
        User actor = User.builder().id(1L).role(Role.ROLE_ADMIN).active(true).build();
        User persisted = User.builder().id(2L).username("old").email("old@example.com")
                .password("encodedOldPassword").role(Role.ROLE_USER).active(true).build();
        User request = User.builder().id(2L).username("changed").email("changed@example.com")
                .password("").role(Role.ROLE_ADMIN).active(true).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(actor));
        when(userRepository.findById(2L)).thenReturn(Optional.of(persisted));
        when(userRepository.findByUsername("changed")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("changed@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.updateAdmin(request, 1L, null);
        assertEquals("changed", result.getUsername());
        assertEquals(Role.ROLE_ADMIN, result.getRole());
        assertEquals("encodedOldPassword", result.getPassword());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("deactivate: usuario normal -> lo desactiva")
    void deactivate_normalUser_setsActiveFalse() {
        User user = User.builder()
                .id(2L)
                .role(Role.ROLE_USER)
                .active(true)
                .build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        userService.deactivate(2L, 1L);

        assertFalse(user.getActive());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("deactivate: no permite desactivar una cuenta admin")
    void deactivate_adminUser_throwsException() {
        User admin = User.builder()
                .id(2L)
                .role(Role.ROLE_ADMIN)
                .active(true)
                .build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> userService.deactivate(2L, 1L)
        );

        assertTrue(ex.getMessage().contains("administrador"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("activate: usuario existente -> lo activa")
    void activate_existingUser_setsActiveTrue() {
        User user = User.builder()
                .id(2L)
                .active(false)
                .build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        userService.activate(2L);

        assertTrue(user.getActive());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("findStatsById: devuelve estadísticas de reviews y tickets pagados")
    void findStatsById_returnsUserStats() {
        Long userId = 1L;

        Review review = Review.builder().id(10L).build();
        Ticket ticket = Ticket.builder()
                .id(20L)
                .status(BuyStatus.PAGADO)
                .build();

        when(reviewRepository.countByUser_Id(userId)).thenReturn(1L);
        when(reviewRepository.findByUser_IdOrderByCreationDateDesc(userId))
                .thenReturn(List.of(review));
        when(ticketRepository.findByUser_IdAndStatus(userId, BuyStatus.PAGADO))
                .thenReturn(List.of(ticket));

        UserStatsDTO stats = userService.findStatsById(userId);

        assertEquals(1L, stats.countReviews());
        assertEquals(1, stats.reviews().size());
        assertEquals(1, stats.tickets().size());
        assertEquals(BuyStatus.PAGADO, stats.tickets().getFirst().getStatus());
    }
}
package com.demo.controller;

import com.demo.model.Movie;
import com.demo.model.Room;
import com.demo.model.Session;
import com.demo.model.Ticket;
import com.demo.model.User;
import com.demo.model.enums.BuyStatus;
import com.demo.model.enums.Role;
import com.demo.repository.MovieRepository;
import com.demo.repository.RoomRepository;
import com.demo.repository.SessionRepository;
import com.demo.repository.TicketRepository;
import com.demo.repository.UserRepository;
import jakarta.servlet.ServletException;
import org.springframework.transaction.annotation.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Ticket ticket1;
    private Ticket ticket2;
    private Ticket ticket3;
    private Session session;
    private User user;

    @BeforeEach
    void setUp() {
        // Limpieza de datos respetando restricciones de integridad referencial
//        ticketRepository.deleteAll();
//        sessionRepository.deleteAll();
//        roomRepository.deleteAll();
//        movieRepository.deleteAll();
//        userRepository.deleteAll();

        // 1. Crear entidades requeridas de apoyo
        Movie movie = movieRepository.save(Movie.builder().title("Batman").active(true).build());
        Room room = roomRepository.save(Room.builder().name("Sala 1").capacity(50).active(true).build());

        // !!! CORRECCIÓN AQUÍ: Agrega los campos que exige la validación de tu entidad User
        user = userRepository.save(User.builder()
                .username("cliente_test")
                .email("test@cinema.com")
                .firstName("Juan")          // <--- Requerido por la validación ("El nombre es obligatorio")
                .password(passwordEncoder.encode("user"))         // <--- Requerido por la validación ("La contraseña es obligatoria")
                .role(com.demo.model.enums.Role.ROLE_ADMIN) // Asegúrate de asignar el rol si tu enumerado o flujo lo requiere
                .build());

        session = sessionRepository.save(Session.builder()
                .movie(movie)
                .room(room)
                .price(10.0)
                .build());

        // 2. Población base de tickets para las pruebas
        ticket1 = ticketRepository.save(Ticket.builder()
                .session(session)
                .row("A") // Nota: Cambiado de .row() a .fila() para coincidir con la columna hibernate de tus logs
                .seat("1")
                .price(10.0)
                .user(user)
                .status(BuyStatus.PAGADO)
                .build());

        ticket2 = ticketRepository.save(Ticket.builder()
                .session(session)
                .row("A")
                .seat("2")
                .price(10.0)
                .status(BuyStatus.PAGADO)
                .build());

        ticket3 = ticketRepository.save(Ticket.builder()
                .session(session)
                .row("B")
                .seat("7")
                .price(10.0)
                .status(BuyStatus.LIBRE)
                .build());
    }

    @Test
    void getTicketsFull() throws Exception {
        // Verifica que cargue correctamente la lista con los 2 tickets iniciales
        mockMvc.perform(get("/tickets").with(user(user)))
                .andExpect(status().isOk())
                .andExpect(view().name("tickets/ticket-list"))
                .andExpect(model().attributeExists("tickets"))
                .andExpect(model().attribute("tickets", hasSize(2)))
                .andExpect(model().attribute("numTickets", 2))
                .andExpect(model().attribute("title", "Entradas vendidas"));
    }

    @Test
    void getTicketsEmpty() throws Exception {
        ticketRepository.deleteAll();

        mockMvc.perform(get("/tickets").with(user(user)))
                .andExpect(status().isOk())
                .andExpect(view().name("tickets/ticket-list"))
                .andExpect(model().attribute("tickets", hasSize(0)))
                .andExpect(model().attribute("numTickets", 0));
    }

    @Test
    // @WithMockUser(username = "admin_test", roles = {"ADMIN"}) // <--- Simula un Administrador
    @DisplayName("GET /tickets/{id}")
    void ticketDetailFound() throws Exception {
        Long ticketId = ticket1.getId();

        mockMvc.perform(get("/tickets/" + ticketId).with(user(user)))
                .andExpect(status().isOk())
                .andExpect(view().name("tickets/ticket-detail"))
                .andExpect(model().attributeExists("ticket"))
                .andExpect(model().attribute("ticket", hasProperty("id", is(ticketId))))
                .andExpect(model().attribute("ticket", hasProperty("row", is("A"))))
                .andExpect(model().attribute("ticket", hasProperty("seat", is("1"))));
    }

    @Test
    void verEdicionTicket() throws Exception {
        Long ticketId = ticket1.getId();

        // El endpoint GET tickets/edit/{id} debe inyectar el ticket, la lista de usuarios y las sesiones
        mockMvc.perform(get("/tickets/edit/" + ticketId).with(user(user)))
                .andExpect(status().isOk())
                .andExpect(view().name("tickets/ticket-form"))
                .andExpect(model().attributeExists("ticket"))
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attributeExists("sessions"))
                // findAll() puede contener datos de otras clases de test (H2 en memoria
                // compartido), así que verificamos que el modelo CONTIENE el usuario y la
                // sesión esperados en vez de afirmar un tamaño absoluto (frágil).
                .andExpect(model().attribute("users", hasItem(hasProperty("id", is(user.getId())))))
                .andExpect(model().attribute("sessions", hasItem(hasProperty("id", is(session.getId())))));
    }


    @Test
    @WithMockUser(username = "cliente_test", roles = {"USER"})
    @DisplayName("GET /tickets/buy/{id} redirige al checkout e inicia la compra")
    void buyTicketSuccess() throws Exception {

        Long ticketId = ticket3.getId();

        assertEquals(BuyStatus.LIBRE, ticket3.getStatus());
        assertNotNull(ticket3.getBuyDateTime());

        mockMvc.perform(get("/tickets/buy/" + ticketId)
                        .with(user(user)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tickets/" + ticketId + "/checkout"));

        Ticket updated = ticketRepository.findById(ticketId).orElseThrow();

        assertEquals(BuyStatus.LIBRE, updated.getStatus());
        assertNotNull(updated.getBuyDateTime());
    }

    @Test
    @DisplayName("POST /tickets/save crea un nuevo ticket")
    void saveTicketCreate() throws Exception {
        long before = ticketRepository.count();

        mockMvc.perform(post("/tickets/save")
                        .with(user(user))
                        .with(csrf())
                        .param("row", "B")
                        .param("seat", "5")
                        .param("price", "12.50")
                        .param("discount", "0")
                        .param("status", BuyStatus.LIBRE.name())
                        .param("session", session.getId().toString())
                        .param("user", user.getId().toString())
                        .param("qrCode", "ONLYFILM-NEW-QR"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tickets"));

        assertEquals(before + 1, ticketRepository.count());

        Ticket saved = ticketRepository.findAll().stream()
                .filter(ticket -> "B".equals(ticket.getRow()) && "5".equals(ticket.getSeat()))
                .findFirst()
                .orElseThrow();

        assertEquals(12.50, saved.getPrice());
        assertEquals(0.0, saved.getDiscount());
        assertEquals(BuyStatus.LIBRE, saved.getStatus());
        assertEquals(session.getId(), saved.getSession().getId());
        assertEquals(user.getId(), saved.getUser().getId());
        assertEquals("ONLYFILM-NEW-QR", saved.getQrCode());
    }

    @Test
    @DisplayName("POST /tickets/save actualiza un ticket existente")
    void saveTicketUpdate() throws Exception {
        Long ticketId = ticket2.getId();

        mockMvc.perform(post("/tickets/save")
                        .with(user(user))
                        .with(csrf())
                        .param("id", ticketId.toString())
                        .param("row", "C")
                        .param("seat", "9")
                        .param("price", "15.00")
                        .param("discount", "2.00")
                        .param("status", BuyStatus.PAGADO.name())
                        .param("session", session.getId().toString())
                        .param("user", user.getId().toString())
                        .param("qrCode", "ONLYFILM-UPDATED-QR"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tickets"));

        Ticket updated = ticketRepository.findById(ticketId).orElseThrow();

        assertEquals("C", updated.getRow());
        assertEquals("9", updated.getSeat());
        assertEquals(15.00, updated.getPrice());
        assertEquals(2.00, updated.getDiscount());
        assertEquals(BuyStatus.PAGADO, updated.getStatus());
        assertEquals(session.getId(), updated.getSession().getId());
        assertEquals(user.getId(), updated.getUser().getId());
        assertEquals("ONLYFILM-UPDATED-QR", updated.getQrCode());
    }

    @Test
    @DisplayName("GET /tickets como usuario normal muestra solo sus entradas")
    void getTicketsAsRegularUserShowsOnlyOwnTickets() throws Exception {
        User regularUser = userRepository.save(User.builder()
                .username("regular_user")
                .email("regular@cinema.com")
                .firstName("Regular")
                .password(passwordEncoder.encode("user123"))
                .role(Role.ROLE_USER)
                .active(true)
                .build());

        Ticket ownTicket = ticketRepository.save(Ticket.builder()
                .session(session)
                .row("C")
                .seat("1")
                .price(10.0)
                .user(regularUser)
                .status(BuyStatus.PAGADO)
                .build());

        ticketRepository.save(Ticket.builder()
                .session(session)
                .row("C")
                .seat("2")
                .price(10.0)
                .user(user)
                .status(BuyStatus.PAGADO)
                .build());

        mockMvc.perform(get("/tickets").with(user(regularUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("tickets/ticket-list"))
                .andExpect(model().attributeExists("tickets"))
                .andExpect(model().attribute("tickets", hasSize(1)))
                .andExpect(model().attribute("tickets", hasItem(hasProperty("id", is(ownTicket.getId())))))
                .andExpect(model().attribute("numTickets", 1))
                .andExpect(model().attribute("title", "Mis entradas"))
                .andExpect(model().attribute("isAdmin", false));
    }

    @Test
    @DisplayName("GET /tickets/{id}/checkout muestra la pantalla de checkout")
    void checkoutGetShowsCheckoutPage() throws Exception {
        Long ticketId = ticket3.getId();

        mockMvc.perform(get("/tickets/" + ticketId + "/checkout").with(user(user)))
                .andExpect(status().isOk())
                .andExpect(view().name("tickets/ticket-checkout"))
                .andExpect(model().attributeExists("ticket"))
                .andExpect(model().attribute("ticket", hasProperty("id", is(ticketId))))
                .andExpect(model().attribute("ticket", hasProperty("status", is(BuyStatus.LIBRE))));
    }

    @Test
    @DisplayName("POST /tickets/{id}/checkout procesa compra sin snacks")
    void checkoutPostWithoutSnacksMarksTicketAsPaid() throws Exception {
        Long ticketId = ticket3.getId();

        mockMvc.perform(post("/tickets/" + ticketId + "/checkout")
                        .with(user(user))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tickets/" + ticketId))
                .andExpect(flash().attribute("successMessage", "Disfruta la película 🎬"));

        Ticket updated = ticketRepository.findById(ticketId).orElseThrow();

        assertEquals(BuyStatus.PAGADO, updated.getStatus());
        assertEquals(user.getId(), updated.getUser().getId());
        assertEquals(session.getPrice(), updated.getPrice());
        assertNull(updated.getSnackPrice());
        assertNotNull(updated.getBuyDateTime());
        assertNotNull(updated.getQrCode());
        assertTrue(updated.getQrCode().startsWith("ONLYFILM-" + ticketId + "-"));
    }

    @Test
    @DisplayName("POST /tickets/{id}/checkout calcula correctamente el precio de snacks")
    void checkoutPostWithSnacksCalculatesSnackPrice() throws Exception {
        Long ticketId = ticket3.getId();

        mockMvc.perform(post("/tickets/" + ticketId + "/checkout")
                        .with(user(user))
                        .with(csrf())
                        .param("qtyPalomitasMedianas", "1")
                        .param("qtyPalomitasGrandes", "1")
                        .param("qtyRefresco", "2")
                        .param("qtyPackDuo", "1")
                        .param("qtyComboFamiliar", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tickets/" + ticketId));

        Ticket updated = ticketRepository.findById(ticketId).orElseThrow();

        double expectedSnackPrice = 4.50 + 6.00 + 6.00 + 8.50 + 14.00;

        assertEquals(BuyStatus.PAGADO, updated.getStatus());
        assertEquals(expectedSnackPrice, updated.getSnackPrice());
        assertEquals(user.getId(), updated.getUser().getId());
        assertNotNull(updated.getQrCode());
    }

    @Test
    @DisplayName("GET /tickets/{id} con ticket inexistente responde 404")
    void ticketDetailNotFoundThrowsNoSuchElementException() {
        Long nonExistingId = 999999L;

        mockMvc.perform(get("/tickets/" + nonExistingId).with(user(user)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /tickets/edit/{id} con ticket inexistente responde 404")
    void editTicketNotFoundThrowsNoSuchElementException() {
        Long nonExistingId = 999999L;

        mockMvc.perform(get("/tickets/edit/" + nonExistingId).with(user(user)))
                .andExpect(status().isNotFound());
    }

}
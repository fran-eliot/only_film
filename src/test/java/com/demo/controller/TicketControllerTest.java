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
    void editFreeTicketShowsDiscountOnlyForm() throws Exception {
        mockMvc.perform(get("/tickets/edit/" + ticket3.getId()).with(user(user)))
                .andExpect(status().isOk())
                .andExpect(view().name("tickets/ticket-form"))
                .andExpect(model().attributeExists("ticket", "ticketDetails"))
                .andExpect(model().attribute("ticket", hasProperty("id", is(ticket3.getId()))));
    }

    @Test
    void editingPaidTicketReturnsConflict() throws Exception {
        mockMvc.perform(get("/tickets/edit/" + ticket1.getId()).with(user(user)))
                .andExpect(status().isConflict());
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
    void adminCannotCreateTicketsManually() throws Exception {
        long before = ticketRepository.count();
        mockMvc.perform(post("/tickets/save").with(user(user)).with(csrf())
                        .param("discount", "0")
                        .param("row", "B").param("seat", "5")
                        .param("status", BuyStatus.PAGADO.name())
                        .param("session", session.getId().toString()))
                .andExpect(status().isBadRequest());
        assertEquals(before, ticketRepository.count());
    }

    @Test
    void adminChangesOnlyFreeTicketDiscountDespiteForgedFields() throws Exception {
        Long id = ticket3.getId();
        mockMvc.perform(post("/tickets/save").with(user(user)).with(csrf())
                        .param("id", id.toString()).param("discount", "2.00")
                        .param("row", "X").param("seat", "99")
                        .param("status", BuyStatus.PAGADO.name())
                        .param("price", "999.00")
                        .param("qrCode", "FORGED")
                        .param("user.id", user.getId().toString())
                        .param("session.id", session.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tickets"));
        Ticket persisted = ticketRepository.findById(id).orElseThrow();
        assertEquals("B", persisted.getRow());
        assertEquals("7", persisted.getSeat());
        assertEquals(10.0, persisted.getPrice());
        assertEquals(2.0, persisted.getDiscount());
        assertEquals(BuyStatus.LIBRE, persisted.getStatus());
        assertNull(persisted.getQrCode());
        assertNull(persisted.getUser());
        assertEquals(session.getId(), persisted.getSession().getId());
    }

    @Test
    void adminCannotChangePaidTicket() throws Exception {
        mockMvc.perform(post("/tickets/save").with(user(user)).with(csrf())
                        .param("id", ticket1.getId().toString()).param("discount", "3.00"))
                .andExpect(status().isConflict());
        Ticket persisted = ticketRepository.findById(ticket1.getId()).orElseThrow();
        assertEquals(BuyStatus.PAGADO, persisted.getStatus());
        assertEquals(user.getId(), persisted.getUser().getId());
    }

    @Test
    void adminCannotDiscountMoreThanPrice() throws Exception {
        mockMvc.perform(post("/tickets/save").with(user(user)).with(csrf())
                        .param("id", ticket3.getId().toString()).param("discount", "11.00"))
                .andExpect(status().isBadRequest());
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

        ticketRepository.save(Ticket.builder()
                .session(session).row("D").seat("1").price(10.0)
                .user(regularUser).status(BuyStatus.CANCELADO).build());
        ticketRepository.save(Ticket.builder()
                .session(session).row("D").seat("2").price(10.0)
                .user(regularUser).status(BuyStatus.LIBRE).build());
        ticketRepository.save(Ticket.builder()
                .session(session).row("D").seat("3").price(10.0)
                .user(regularUser).status(BuyStatus.INICIADO).build());

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
    void checkoutGetRejectsInactiveSession() throws Exception {
        session.setActive(false);
        sessionRepository.saveAndFlush(session);
        mockMvc.perform(get("/tickets/" + ticket3.getId() + "/checkout").with(user(user)))
                .andExpect(status().isConflict());
        assertEquals(BuyStatus.LIBRE, ticketRepository.findById(ticket3.getId()).orElseThrow().getStatus());
    }

    @Test
    void checkoutPostRejectsInactiveSessionWithoutSellingTicket() throws Exception {
        session.setActive(false);
        sessionRepository.saveAndFlush(session);
        mockMvc.perform(post("/tickets/" + ticket3.getId() + "/checkout")
                        .with(user(user)).with(csrf()))
                .andExpect(status().isConflict());
        Ticket persisted = ticketRepository.findById(ticket3.getId()).orElseThrow();
        assertEquals(BuyStatus.LIBRE, persisted.getStatus());
        assertNull(persisted.getUser());
        assertNull(persisted.getQrCode());
    }

    @Test
    void checkoutGetRejectsInactiveMovie() throws Exception {
        Movie movie = session.getMovie();
        movie.setActive(false);
        movieRepository.saveAndFlush(movie);
        mockMvc.perform(get("/tickets/" + ticket3.getId() + "/checkout").with(user(user)))
                .andExpect(status().isConflict());
        assertEquals(BuyStatus.LIBRE, ticketRepository.findById(ticket3.getId()).orElseThrow().getStatus());
    }

    @Test
    void checkoutPostRejectsInactiveMovieWithoutSellingTicket() throws Exception {
        Movie movie = session.getMovie();
        movie.setActive(false);
        movieRepository.saveAndFlush(movie);
        mockMvc.perform(post("/tickets/" + ticket3.getId() + "/checkout")
                        .with(user(user)).with(csrf()))
                .andExpect(status().isConflict());
        Ticket persisted = ticketRepository.findById(ticket3.getId()).orElseThrow();
        assertEquals(BuyStatus.LIBRE, persisted.getStatus());
        assertNull(persisted.getUser());
        assertNull(persisted.getQrCode());
    }

    @Test
    void checkoutGetRejectsInactiveRoom() throws Exception {
        Room room = session.getRoom();
        room.setActive(false);
        roomRepository.saveAndFlush(room);
        mockMvc.perform(get("/tickets/" + ticket3.getId() + "/checkout").with(user(user)))
                .andExpect(status().isConflict());
        assertEquals(BuyStatus.LIBRE, ticketRepository.findById(ticket3.getId()).orElseThrow().getStatus());
    }

    @Test
    void checkoutPostRejectsInactiveRoomWithoutSellingTicket() throws Exception {
        Room room = session.getRoom();
        room.setActive(false);
        roomRepository.saveAndFlush(room);
        mockMvc.perform(post("/tickets/" + ticket3.getId() + "/checkout")
                        .with(user(user)).with(csrf()))
                .andExpect(status().isConflict());
        Ticket persisted = ticketRepository.findById(ticket3.getId()).orElseThrow();
        assertEquals(BuyStatus.LIBRE, persisted.getStatus());
        assertNull(persisted.getUser());
        assertNull(persisted.getQrCode());
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
    void ticketDetailNotFoundThrowsNoSuchElementException() throws Exception {
        Long nonExistingId = 999999L;

        mockMvc.perform(get("/tickets/" + nonExistingId).with(user(user)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /tickets/edit/{id} con ticket inexistente responde 404")
    void editTicketNotFoundThrowsNoSuchElementException() throws Exception {
        Long nonExistingId = 999999L;

        mockMvc.perform(get("/tickets/edit/" + nonExistingId).with(user(user)))
                .andExpect(status().isNotFound());
    }

}
package com.demo.controller;

import com.demo.repository.MovieRepository;
import com.demo.repository.SessionRepository;
import com.demo.repository.RoomRepository;
import com.demo.repository.TicketRepository;
import com.demo.model.Movie;
import com.demo.model.Room;
import com.demo.model.Ticket;
import com.demo.model.enums.BuyStatus;
import com.demo.model.Ticket;
import com.demo.model.enums.BuyStatus;
import java.time.LocalDateTime;
import java.time.ZoneId;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
public class SessionControllerTest {
    @Autowired
    SessionRepository sessionRepository;
    @Autowired
    MovieRepository movieRepository;
    @Autowired
    RoomRepository roomRepository;
    @Autowired
    TicketRepository ticketRepository;
    @Autowired
    MockMvc mockMvc;

    @Test
    void listTableHeadersUseCssWidths() throws Exception {
        mockMvc.perform(get("/sessions"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("style=\"width: 30%;\"")))
                .andExpect(content().string(not(containsString("scope=\"col\" width="))));
    }

    @Test
    void sessionEmpty() throws Exception {
        mockMvc.perform(get("/sessions"))
                .andExpect(status().isOk())
                .andExpect(view().name("sessions/session-list"))
                .andExpect(model().attributeExists("proyecciones"))
                .andExpect(model().attribute("proyecciones", hasSize(0)))
                .andExpect(model().attribute("proyeccionesCount", is(0L)));
    }

    @Test
    void sessionFull() throws Exception {
        sessionRepository.save(com.demo.model.Session.builder().build());
        sessionRepository.save(com.demo.model.Session.builder().build());
        mockMvc.perform(get("/sessions"))
                .andExpect(status().isOk())
                .andExpect(view().name("sessions/session-list"))
                .andExpect(model().attributeExists("proyecciones"))
                .andExpect(model().attribute("proyecciones", hasSize(2)))
                .andExpect(model().attribute("proyeccionesCount", is(2L)));
    }

    @Test
    void newSession() throws Exception {
        mockMvc.perform(get("/sessions/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("sessions/session-form"))
                .andExpect(model().attributeExists("proyeccion"))
                .andExpect(model().attributeExists("movies"))
                .andExpect(model().attributeExists("rooms"));
    }

    @Test
    void sessionDetail() throws Exception {
        var movie = movieRepository.save(Movie.builder()
                .title("Test movie")
                .active(true)
                .build());

        var room = roomRepository.save(Room.builder()
                .name("Sala test")
                .capacity(10)
                .build());


        var session = sessionRepository.save(com.demo.model.Session.builder()
                .room(room)
                .movie(movie)   // ← esto es lo que faltaba
                .build());

        mockMvc.perform(get("/sessions/" + session.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("sessions/session-detail"))
                .andExpect(model().attributeExists("tickets"))
                .andExpect(model().attributeExists("seatsPerRow"));
    }

    @Test
    void sessionDetailWithoutRoomRendersSafely() throws Exception {
        var session = sessionRepository.save(com.demo.model.Session.builder().build());

        mockMvc.perform(get("/sessions/" + session.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("sessions/session-detail"))
                .andExpect(model().attribute("seatsPerRow", 0))
                .andExpect(content().string(containsString("Sin sala")))
                .andExpect(content().string(containsString("href=\"/sessions\"")));
    }

    @Test
    void sessionDetailWithValidCapacityPreservesSeatLayout() throws Exception {
        var room = roomRepository.save(Room.builder()
                .name("Sala con capacidad").capacity(16).build());
        var session = sessionRepository.save(com.demo.model.Session.builder()
                .room(room).build());

        mockMvc.perform(get("/sessions/" + session.getId()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("seatsPerRow", 4))
                .andExpect(model().attributeExists("tickets"));
    }

    @Test
    void futureSessionOffersFreeSeatButNotCancelledSeat() throws Exception {
        Movie movie = movieRepository.save(Movie.builder().title("Future film").active(true).build());
        Room room = roomRepository.save(Room.builder().name("Sala futura").capacity(10).active(true).build());
        var session = sessionRepository.save(com.demo.model.Session.builder()
                .movie(movie).room(room).price(10.0)
                .startTime(LocalDateTime.now(ZoneId.of("Europe/Madrid")).plusDays(1)).build());
        ticketRepository.save(Ticket.builder().session(session).row("A").seat("1").status(BuyStatus.LIBRE).build());
        ticketRepository.save(Ticket.builder().session(session).row("A").seat("2").status(BuyStatus.CANCELADO).build());

        mockMvc.perform(get("/sessions/" + session.getId()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("salesOpen", true))
                .andExpect(content().string(containsString("data-bs-target=\"#confirmarCompraModal\"")))
                .andExpect(content().string(containsString("seat-btn taken")));
    }

    @Test
    void startedSessionDoesNotOfferCheckoutAction() throws Exception {
        Movie movie = movieRepository.save(Movie.builder().title("Past film").active(true).build());
        Room room = roomRepository.save(Room.builder().name("Sala pasada").capacity(10).active(true).build());
        var session = sessionRepository.save(com.demo.model.Session.builder()
                .movie(movie).room(room).price(10.0)
                .startTime(LocalDateTime.now(ZoneId.of("Europe/Madrid")).minusMinutes(5)).build());
        ticketRepository.save(Ticket.builder().session(session).row("A").seat("1").status(BuyStatus.LIBRE).build());

        mockMvc.perform(get("/sessions/" + session.getId()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("salesOpen", false))
                .andExpect(content().string(containsString("La venta de entradas no está disponible")))
                .andExpect(content().string(not(containsString("data-bs-target=\"#confirmarCompraModal\""))));
    }

    @Test
    void missingSessionDetailReturnsNotFound() throws Exception {
        mockMvc.perform(get("/sessions/999999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void missingSessionEditReturnsNotFound() throws Exception {
        mockMvc.perform(get("/sessions/edit/999999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void editSession() throws Exception {
        var session = sessionRepository.save(com.demo.model.Session.builder().build());
        mockMvc.perform(get("/sessions/edit/" + session.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("sessions/session-form"))
                .andExpect(model().attributeExists("proyeccion"))
                .andExpect(model().attributeExists("movies"))
                .andExpect(model().attributeExists("rooms"));
    }

    @Test
    void deactivateSession() throws Exception {
        var session = sessionRepository.save(com.demo.model.Session.builder().build());
        mockMvc.perform(post("/sessions/deactivate/" + session.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/sessions"));
    }

    @Test
    void deactivationPreservesSoldTicketsAndIsIdempotent() throws Exception {
        var movie = movieRepository.save(Movie.builder().title("Deactivation film").active(true).build());
        var room = roomRepository.save(Room.builder().name("Sala cerrada").capacity(10).active(true).build());
        var session = sessionRepository.save(com.demo.model.Session.builder()
                .movie(movie).room(room).price(10.0).active(true).build());
        var sold = ticketRepository.save(Ticket.builder()
                .session(session).row("A").seat("1").status(BuyStatus.PAGADO).build());

        mockMvc.perform(post("/sessions/deactivate/" + session.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/sessions"));
        mockMvc.perform(post("/sessions/deactivate/" + session.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection());

        org.junit.jupiter.api.Assertions.assertFalse(sessionRepository.findById(session.getId()).orElseThrow().getActive());
        org.junit.jupiter.api.Assertions.assertEquals(BuyStatus.PAGADO,
                ticketRepository.findById(sold.getId()).orElseThrow().getStatus());
    }

    @Test
    void deactivationOfMissingSessionIsIdempotent() throws Exception {
        mockMvc.perform(post("/sessions/deactivate/99999999").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/sessions"));
    }

    @Test
    void createSessionGeneratesTicketsWhenRoomHasCapacity() throws Exception {
        Movie movie = movieRepository.save(Movie.builder()
                .title("Test movie")
                .active(true)
                .build());
        Room room = roomRepository.save(Room.builder()
                .name("Sala 1")
                .capacity(12)
                .active(true)
                .build());

        mockMvc.perform(post("/sessions")
                        .with(csrf())
                        .param("price", "10.0")
                        .param("language", "VOSE")
                        .param("adMinutes", "5")
                        .param("movieId", String.valueOf(movie.getId()))
                        .param("roomId", String.valueOf(room.getId())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/sessions"));

        // It should create 1 session and generate 12 tickets for it.
        var savedSessions = sessionRepository.findAll();
        org.junit.jupiter.api.Assertions.assertEquals(1, savedSessions.size());
        org.junit.jupiter.api.Assertions.assertEquals(12, ticketRepository.findBySession_Id(savedSessions.get(0).getId()).size());
    }

    @Test
    void updateSessionDoesNotGenerateTicketsAgain() throws Exception {
        Room room = roomRepository.save(Room.builder()
                .name("Sala 2")
                .capacity(10)
                .build());
        var existing = sessionRepository.save(com.demo.model.Session.builder()
                .price(9.0)
                .room(room)
                .build());

        mockMvc.perform(post("/sessions")
                        .with(csrf())
                        .param("id", String.valueOf(existing.getId()))
                        .param("price", "11.0")
                        .param("roomId", String.valueOf(room.getId())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/sessions"));

        org.junit.jupiter.api.Assertions.assertEquals(0, ticketRepository.findBySession_Id(existing.getId()).size());
    }
    @Test
    void editingSessionIgnoresForgedActiveFlag() throws Exception {
        var original = sessionRepository.save(com.demo.model.Session.builder()
                .price(7.0).active(true).build());
        mockMvc.perform(post("/sessions")
                        .param("id", original.getId().toString())
                        .param("price", "8.0")
                        .param("active", "false"))
                .andExpect(status().is3xxRedirection());
        var persisted = sessionRepository.findById(original.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(persisted.getActive());
        org.junit.jupiter.api.Assertions.assertEquals(8.0, persisted.getPrice());
    }


    @Test
    void invalidCreationShowsPriceErrorsWithoutSavingSession() throws Exception {
        long before = sessionRepository.count();
        mockMvc.perform(post("/sessions").with(csrf())
                        .param("price", "-1")
                        .param("adMinutes", "5"))
                .andExpect(status().isOk())
                .andExpect(view().name("sessions/session-form"))
                .andExpect(model().attributeHasFieldErrors("proyeccion", "price"))
                .andExpect(model().attributeExists("movies", "rooms"))
                .andExpect(content().string(containsString("priceError")));
        org.junit.jupiter.api.Assertions.assertEquals(before, sessionRepository.count());
    }

    @Test
    void invalidEditShowsAdvertisingErrorsAndPreservesOriginal() throws Exception {
        var original = sessionRepository.save(com.demo.model.Session.builder()
                .price(8.0).adMinutes(10).build());
        mockMvc.perform(post("/sessions").with(csrf())
                        .param("id", original.getId().toString())
                        .param("price", "12.0")
                        .param("adMinutes", "-3"))
                .andExpect(status().isOk())
                .andExpect(view().name("sessions/session-form"))
                .andExpect(model().attributeHasFieldErrors("proyeccion", "adMinutes"))
                .andExpect(model().attributeExists("movies", "rooms"));
        var persisted = sessionRepository.findById(original.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(8.0, persisted.getPrice());
        org.junit.jupiter.api.Assertions.assertEquals(10, persisted.getAdMinutes());
    }
}

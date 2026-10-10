package com.demo.controller;

import com.demo.repository.MovieRepository;
import com.demo.repository.SessionRepository;
import com.demo.repository.RoomRepository;
import com.demo.repository.TicketRepository;
import com.demo.model.Movie;
import com.demo.model.Room;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
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

    @BeforeEach
    void setUp() {
//        ticketRepository.deleteAll();
//        sessionRepository.deleteAll();
//        movieRepository.deleteAll();
//        roomRepository.deleteAll();
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
    void createSessionGeneratesTicketsWhenRoomHasCapacity() throws Exception {
        Movie movie = movieRepository.save(Movie.builder()
                .title("Test movie")
                .active(true)
                .build());
        Room room = roomRepository.save(Room.builder()
                .name("Sala 1")
                .capacity(12)
                .build());

        mockMvc.perform(post("/sessions")
                        .with(csrf())
                        .param("price", "10.0")
                        .param("language", "VOSE")
                        .param("adMinutes", "5")
                        .param("movie.id", String.valueOf(movie.getId()))
                        .param("room.id", String.valueOf(room.getId())))
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
                        .param("room.id", String.valueOf(room.getId())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/sessions"));

        org.junit.jupiter.api.Assertions.assertEquals(0, ticketRepository.findBySession_Id(existing.getId()).size());
    }
}

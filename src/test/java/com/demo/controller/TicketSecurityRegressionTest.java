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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression tests for issue #1.
 * These tests are expected to FAIL until ticket authorization is implemented.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TicketSecurityRegressionTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private MovieRepository movieRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private SessionRepository sessionRepository;
    @Autowired private TicketRepository ticketRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private User owner;
    private User otherUser;
    private Ticket paidTicket;
    private Session session;

    @BeforeEach
    void setUp() {
        Movie movie = movieRepository.save(Movie.builder().title("Regression Movie").active(true).build());
        Room room = roomRepository.save(Room.builder().name("Regression Room").capacity(20).active(true).build());
        session = sessionRepository.save(Session.builder().movie(movie).room(room).price(10.0).build());
        owner = createUser("ticket_owner_regression", "ticket-owner-regression@example.com");
        otherUser = createUser("ticket_other_regression", "ticket-other-regression@example.com");
        paidTicket = ticketRepository.save(Ticket.builder()
                .session(session).row("A").seat("1").price(10.0)
                .user(owner).status(BuyStatus.PAGADO).build());
    }

    private User createUser(String username, String email) {
        return userRepository.save(User.builder()
                .username(username).email(email).firstName("Regression")
                .password(passwordEncoder.encode("test-password"))
                .role(Role.ROLE_USER).active(true).build());
    }

    @Test
    void regularUserCannotReadAnotherUsersTicket() throws Exception {
        mockMvc.perform(get("/tickets/{id}", paidTicket.getId()).with(user(otherUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void regularUserCannotCreateTicketViaAdministrativeEndpoint() throws Exception {
        mockMvc.perform(post("/tickets/save").with(user(otherUser)).with(csrf())
                        .param("row", "B")
                        .param("seat", "2")
                        .param("price", "10.0")
                        .param("status", BuyStatus.LIBRE.name())
                        .param("session", session.getId().toString()))
                .andExpect(status().isForbidden());
    }
}

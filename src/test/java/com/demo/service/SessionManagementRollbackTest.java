package com.demo.service;

import com.demo.dto.SessionForm;
import com.demo.model.Movie;
import com.demo.model.Room;
import com.demo.model.Session;
import com.demo.repository.MovieRepository;
import com.demo.repository.RoomRepository;
import com.demo.repository.SessionRepository;
import com.demo.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

/**
 * Intentionally NOT @Transactional on the test: assertions read committed DB
 * state after the service transaction has rolled back.
 */
@SpringBootTest
class SessionManagementRollbackTest {
    @Autowired SessionManagementService service;
    @Autowired SessionRepository sessions;
    @Autowired MovieRepository movies;
    @Autowired RoomRepository rooms;
    @Autowired TicketRepository tickets;

    @MockitoBean TicketService ticketService;

    @Test
    void failedTicketGenerationRollsBackSessionAndTickets() {
        Movie movie = movies.save(Movie.builder().title("Rollback Test Movie").active(true).build());
        Room room = rooms.save(Room.builder().name("Rollback Room")
                .capacity(12).active(true).build());
        long sessionsBefore = sessions.count();
        long ticketsBefore = tickets.count();

        SessionForm form = new SessionForm();
        form.setMovieId(movie.getId());
        form.setRoomId(room.getId());
        form.setPrice(9.5);

        doThrow(new IllegalStateException("Simulated seat generation failure"))
                .when(ticketService).generarTickets(any(Session.class));

        try {
            assertThrows(IllegalStateException.class, () -> service.save(form));
            assertEquals(sessionsBefore, sessions.count(), "Session must roll back");
            assertEquals(ticketsBefore, tickets.count(), "Tickets must not remain");
        } finally {
            // The failed transaction has finished. Keep unrelated seeded test
            // data intact rather than relying on test method transaction rollback.
            rooms.deleteById(room.getId());
            movies.deleteById(movie.getId());
        }
    }

    @Test
    void rejectsNegativePriceWithoutSavingSession() {
        Movie movie = movies.save(Movie.builder().title("Capacity Test Movie").active(true).build());
        Room room = rooms.save(Room.builder().name("Validation Room")
                .capacity(12).active(true).build());
        long before = sessions.count();

        SessionForm form = new SessionForm();
        form.setMovieId(movie.getId());
        form.setRoomId(room.getId());
        form.setPrice(-1.0);
        try {
            assertThrows(IllegalArgumentException.class, () -> service.save(form));
            assertEquals(before, sessions.count());
        } finally {
            rooms.deleteById(room.getId());
            movies.deleteById(movie.getId());
        }
    }
}

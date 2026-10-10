package com.demo.service;

import com.demo.dto.SessionForm;
import com.demo.model.Movie;
import com.demo.model.Room;
import com.demo.model.Session;
import com.demo.model.Ticket;
import com.demo.model.enums.BuyStatus;
import com.demo.repository.MovieRepository;
import com.demo.repository.RoomRepository;
import com.demo.repository.SessionRepository;
import com.demo.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class SessionEditInvariantsTest {
    @Autowired SessionManagementService management;
    @Autowired SessionRepository sessions;
    @Autowired MovieRepository movies;
    @Autowired RoomRepository rooms;
    @Autowired TicketRepository tickets;

    private Session prepareSession() {
        Movie movie = movies.save(Movie.builder().title("Original").active(true).build());
        Room room = rooms.save(Room.builder().name("Edit Invariant Room")
                .capacity(10).active(true).build());
        SessionForm create = new SessionForm();
        create.setMovieId(movie.getId());
        create.setRoomId(room.getId());
        create.setPrice(10.0);
        create.setLanguage("VO");
        create.setAdMinutes(5);
        create.setStartTime(LocalDateTime.of(2027, 1, 15, 18, 0));
        return management.save(create);
    }

    private SessionForm editForm(Session session) {
        return SessionForm.from(session);
    }

    @Test
    void unsoldInventoryAllowsMovieTimePriceAndLanguageUpdates() {
        Session original = prepareSession();
        List<Ticket> seats = tickets.findBySession_Id(original.getId());
        assertEquals(10, seats.size());

        Movie replacement = movies.save(Movie.builder().title("Replacement").active(true).build());
        SessionForm form = editForm(original);
        form.setMovieId(replacement.getId());
        form.setStartTime(original.getStartTime().plusDays(1));
        form.setPrice(12.5);
        form.setLanguage("VOSE");
        form.setAdMinutes(8);

        management.save(form);

        Session updated = sessions.findById(original.getId()).orElseThrow();
        assertEquals(replacement.getId(), updated.getMovie().getId());
        assertEquals(form.getStartTime(), updated.getStartTime());
        assertEquals(12.5, updated.getPrice());
        assertEquals("VOSE", updated.getLanguage());
        assertEquals(8, updated.getAdMinutes());
        List<Ticket> after = tickets.findBySession_Id(original.getId());
        assertEquals(10, after.size(), "Editing must not regenerate inventory");
        assertTrue(after.stream().allMatch(t -> t.getPrice() == 12.5));
    }

    @Test
    void paidTicketRejectsChangesToAllEssentialFields() {
        Session original = prepareSession();
        List<Ticket> inventory = tickets.findBySession_Id(original.getId());
        Ticket paid = inventory.getFirst();
        paid.setStatus(BuyStatus.PAGADO);
        paid.setQrCode("ALREADY-PAID");
        tickets.saveAndFlush(paid);

        Movie replacement = movies.save(Movie.builder().title("Changed").active(true).build());
        for (int change = 0; change < 5; change++) {
            SessionForm form = editForm(original);
            switch (change) {
                case 0 -> form.setMovieId(replacement.getId());
                case 1 -> form.setStartTime(original.getStartTime().plusHours(1));
                case 2 -> form.setPrice(15.0);
                case 3 -> form.setLanguage("Doblada");
                case 4 -> form.setAdMinutes(20);
                default -> throw new AssertionError();
            }
            assertThrows(IllegalStateException.class, () -> management.save(form));
        }

        assertEquals("Original", sessions.findById(original.getId()).orElseThrow().getMovie().getTitle());
        assertEquals(10.0, original.getPrice());
        assertEquals("ALREADY-PAID", tickets.findById(paid.getId()).orElseThrow().getQrCode());
        assertEquals(10, tickets.findBySession_Id(original.getId()).size());
    }

    @Test
    void paidSessionAllowsSavingUnchangedFields() {
        Session original = prepareSession();
        Ticket paid = tickets.findBySession_Id(original.getId()).getFirst();
        paid.setStatus(BuyStatus.PAGADO);
        tickets.saveAndFlush(paid);
        management.save(editForm(original));
        assertEquals(10, tickets.findBySession_Id(original.getId()).size());
    }

    @Test
    void roomChangeIsRejectedEvenWhenAllTicketsAreFree() {
        Session original = prepareSession();
        Room other = rooms.save(Room.builder().name("Other Room")
                .capacity(10).active(true).build());
        SessionForm form = editForm(original);
        form.setRoomId(other.getId());
        assertThrows(IllegalStateException.class, () -> management.save(form));
        assertEquals(10, tickets.findBySession_Id(original.getId()).size());
        assertNotEquals(other.getId(), original.getRoom().getId());
    }
}

package com.demo.service;

import com.demo.dto.DashboardSalesSummary;
import com.demo.model.Movie;
import com.demo.model.Room;
import com.demo.model.Session;
import com.demo.model.Ticket;
import com.demo.model.enums.BuyStatus;
import com.demo.repository.MovieRepository;
import com.demo.repository.RoomRepository;
import com.demo.repository.SessionRepository;
import com.demo.repository.TicketRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(DashboardMetricsService.class)
class DashboardMetricsServiceTest {
    @Autowired DashboardMetricsService metrics;
    @Autowired MovieRepository movies;
    @Autowired RoomRepository rooms;
    @Autowired SessionRepository sessions;
    @Autowired TicketRepository tickets;

    private static final LocalDateTime FROM = LocalDateTime.of(2026, 10, 1, 0, 0);
    private static final LocalDateTime UNTIL = LocalDateTime.of(2026, 11, 1, 0, 0);

    @Test
    void countsOnlyPaidTicketsAndUsesAlreadyDiscountedPrices() {
        Session a = fixture("Alpha movie", "Sala Alpha");
        Session b = fixture("Beta movie", "Sala Beta");
        add(a, "1", BuyStatus.PAGADO, 8.0, 4.5, FROM.plusDays(2));
        add(a, "2", BuyStatus.PAGADO, 10.0, null, FROM.plusDays(3));
        add(a, "3", BuyStatus.LIBRE, 100.0, 30.0, FROM.plusDays(4));
        add(a, "4", BuyStatus.CANCELADO, 100.0, 30.0, FROM.plusDays(4));
        add(b, "1", BuyStatus.PAGADO, 6.0, 3.0, FROM.plusDays(5));
        add(a, "5", BuyStatus.PAGADO, 5.0, 2.0, UNTIL);
        tickets.flush();

        DashboardSalesSummary all = metrics.salesSummary(FROM, UNTIL, null, null);
        assertEquals(3, all.paidTickets());
        assertEquals(24.0, all.ticketRevenue(), 0.001);
        assertEquals(7.5, all.snackRevenue(), 0.001);
        assertEquals(31.5, all.totalRevenue(), 0.001);

        DashboardSalesSummary filtered = metrics.salesSummary(FROM, UNTIL, a.getMovie().getId(), a.getRoom().getId());
        assertEquals(2, filtered.paidTickets());
        assertEquals(18.0, filtered.ticketRevenue(), 0.001);
        assertEquals(4.5, filtered.snackRevenue(), 0.001);
        assertEquals(0, metrics.salesSummary(FROM, UNTIL, a.getMovie().getId(), b.getRoom().getId()).paidTickets());
    }

    @Test
    void emptyRangeReturnsZerosAndRejectsInvalidFilters() {
        DashboardSalesSummary empty = metrics.salesSummary(FROM, UNTIL, null, null);
        assertEquals(0, empty.paidTickets());
        assertEquals(0.0, empty.ticketRevenue(), 0.001);
        assertEquals(0.0, empty.snackRevenue(), 0.001);
        assertThrows(IllegalArgumentException.class, () -> metrics.salesSummary(UNTIL, FROM, null, null));
        assertThrows(IllegalArgumentException.class, () -> metrics.salesSummary(FROM, UNTIL, -1L, null));
    }

    private Session fixture(String movieTitle, String roomName) {
        Movie movie = movies.save(Movie.builder().title(movieTitle).active(true).build());
        Room room = rooms.save(Room.builder().name(roomName).capacity(20).active(true).build());
        return sessions.save(Session.builder().movie(movie).room(room).price(10.0)
                .startTime(UNTIL.plusDays(2)).active(true).build());
    }

    private void add(Session session, String seat, BuyStatus status, Double price, Double snacks, LocalDateTime bought) {
        tickets.save(Ticket.builder().session(session).row("A").seat(seat)
                .status(status).price(price).snackPrice(snacks).discount(0.0)
                .buyDateTime(bought).build());
    }
}

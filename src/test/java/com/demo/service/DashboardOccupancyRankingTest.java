package com.demo.service;

import com.demo.model.*;
import com.demo.model.enums.BuyStatus;
import com.demo.repository.*;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(DashboardMetricsService.class)
class DashboardOccupancyRankingTest {
    @Autowired DashboardMetricsService metrics;
    @Autowired MovieRepository movies;
    @Autowired RoomRepository rooms;
    @Autowired SessionRepository sessions;
    @Autowired TicketRepository tickets;

    private static final LocalDateTime FROM = LocalDateTime.of(2026, 10, 1, 0, 0);
    private static final LocalDateTime UNTIL = LocalDateTime.of(2026, 11, 1, 0, 0);

    @Test
    void ocupacionIncluyeTodasLasButacasYSoloCuentaPagadasComoVendidas() {
        Session session = session("Ocupacion", "Sala ocupacion");
        ticket(session, "1", BuyStatus.PAGADO, FROM.plusDays(1));
        ticket(session, "2", BuyStatus.PAGADO, FROM.plusDays(1));
        ticket(session, "3", BuyStatus.LIBRE, FROM.plusDays(1));
        ticket(session, "4", BuyStatus.CANCELADO, FROM.plusDays(1));
        tickets.flush();

        var occupancy = metrics.sessionOccupancy(session.getId());
        assertEquals(4, occupancy.totalSeats());
        assertEquals(2, occupancy.paidSeats());
        assertEquals(50.0, occupancy.occupancyPercent(), 0.001);
        assertNull(metrics.sessionOccupancy(Long.MAX_VALUE).occupancyPercent());
        assertThrows(IllegalArgumentException.class, () -> metrics.sessionOccupancy(null));
    }

    @Test
    void rankingSoloIncluyeVentasYAplicaFiltroDeSala() {
        Session first = session("Pelicula A", "Sala A");
        Session second = session("Pelicula B", "Sala B");
        ticket(first, "1", BuyStatus.PAGADO, FROM.plusDays(1));
        ticket(first, "2", BuyStatus.PAGADO, FROM.plusDays(2));
        ticket(second, "1", BuyStatus.PAGADO, FROM.plusDays(3));
        ticket(second, "2", BuyStatus.LIBRE, FROM.plusDays(3));
        ticket(second, "3", BuyStatus.PAGADO, UNTIL);
        tickets.flush();

        var ranking = metrics.movieRanking(FROM, UNTIL, null);
        assertEquals(2, ranking.size());
        assertEquals(first.getMovie().getId(), ranking.get(0).movieId());
        assertEquals(2, ranking.get(0).paidTickets());
        assertEquals(1, ranking.get(1).paidTickets());
        var filtered = metrics.movieRanking(FROM, UNTIL, second.getRoom().getId());
        assertEquals(1, filtered.size());
        assertEquals(second.getMovie().getId(), filtered.get(0).movieId());
        assertThrows(IllegalArgumentException.class, () -> metrics.movieRanking(UNTIL, FROM, null));
    }

    private Session session(String title, String name) {
        Movie movie = movies.save(Movie.builder().title(title).active(true).build());
        Room room = rooms.save(Room.builder().name(name).capacity(20).active(true).build());
        return sessions.save(Session.builder().movie(movie).room(room).price(10.0)
                .startTime(UNTIL.plusDays(2)).active(true).build());
    }

    private void ticket(Session session, String seat, BuyStatus status, LocalDateTime purchased) {
        tickets.save(Ticket.builder().session(session).row("A").seat(seat)
                .status(status).price(10.0).discount(0.0).buyDateTime(purchased).build());
    }
}

package com.demo.service;

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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Issue #1: verifies that only one buyer can acquire a given seat.
 * Intentionally not @Transactional: worker threads need committed fixtures.
 */
@SpringBootTest
class TicketCheckoutConcurrencyTest {

    @Autowired TicketService ticketService;
    @Autowired TicketRepository ticketRepository;
    @Autowired UserRepository userRepository;
    @Autowired SessionRepository sessionRepository;
    @Autowired MovieRepository movieRepository;
    @Autowired RoomRepository roomRepository;

    @Test
    void onlyOneOfTwoConcurrentBuyersCanPurchaseTheSameSeat() throws Exception {
        String suffix = java.util.UUID.randomUUID().toString();
        Movie movie = movieRepository.save(Movie.builder()
                .title("Concurrency " + suffix).active(true).build());
        Room room = roomRepository.save(Room.builder()
                .name("Room " + suffix).capacity(10).active(true).build());
        Session session = sessionRepository.save(Session.builder()
                .movie(movie).room(room).price(10.0)
                .startTime(LocalDateTime.now(ZoneId.of("Europe/Madrid")).plusDays(1)).build());
        User first = userRepository.save(User.builder()
                .username("buyer1_" + suffix).email("buyer1_" + suffix + "@example.com")
                .firstName("Buyer 1").password("not-used-in-test")
                .role(Role.ROLE_USER).active(true).build());
        User second = userRepository.save(User.builder()
                .username("buyer2_" + suffix).email("buyer2_" + suffix + "@example.com")
                .firstName("Buyer 2").password("not-used-in-test")
                .role(Role.ROLE_USER).active(true).build());
        Ticket ticket = ticketRepository.save(Ticket.builder()
                .session(session).row("A").seat("1").status(BuyStatus.LIBRE).build());

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> firstResult = executor.submit(() -> attemptPurchase(ticket.getId(), first, ready, start));
            Future<Boolean> secondResult = executor.submit(() -> attemptPurchase(ticket.getId(), second, ready, start));
            assertTrue(ready.await(10, TimeUnit.SECONDS), "Both buyers should be ready");
            start.countDown();

            int successfulPurchases = (firstResult.get(15, TimeUnit.SECONDS) ? 1 : 0)
                    + (secondResult.get(15, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, successfulPurchases, "Exactly one checkout must succeed");
            Ticket persisted = ticketRepository.findById(ticket.getId()).orElseThrow();
            assertEquals(BuyStatus.PAGADO, persisted.getStatus());
            assertNotNull(persisted.getUser());
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private boolean attemptPurchase(Long id, User buyer, CountDownLatch ready,
                                    CountDownLatch start) throws InterruptedException {
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out waiting for concurrent start");
        }
        try {
            ticketService.processCheckout(id, buyer, 0, 0, 0, 0, 0);
            return true;
        } catch (IllegalStateException _) {
            return false;
        }
    }
}

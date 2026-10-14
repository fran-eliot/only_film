package com.demo.repository;

import com.demo.model.Movie;
import com.demo.model.Room;
import com.demo.model.Session;
import com.demo.model.Ticket;
import com.demo.model.User;
import com.demo.model.enums.BuyStatus;
import com.demo.model.enums.Role;
import com.demo.service.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Executes only with a real PostgreSQL service.
 * The postgres profile runs Flyway V1 and Hibernate schema validation.
 * Not transactional at test level: concurrency threads need committed fixtures.
 */
@SpringBootTest
@ActiveProfiles("postgres")
class PostgresPersistenceIntegrationTest {

    @Autowired MovieRepository movies;
    @Autowired RoomRepository rooms;
    @Autowired SessionRepository sessions;
    @Autowired TicketRepository tickets;
    @Autowired UserRepository users;
    @Autowired TicketService checkout;
    @Autowired JdbcTemplate jdbc;

    private static String unique() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private User createBuyer(String suffix) {
        return users.save(User.builder()
                .username("pg_" + suffix)
                .email("pg_" + suffix + "@example.com")
                .firstName("Buyer")
                .password("test-password-not-used")
                .role(Role.ROLE_USER).active(true).build());
    }

    private Ticket createAvailableTicket(String suffix) {
        Movie movie = movies.save(Movie.builder().title("Postgres movie " + suffix).active(true).build());
        Room room = rooms.save(Room.builder().name("PG room " + suffix).capacity(20).active(true).build());
        Session session = sessions.save(Session.builder().movie(movie).room(room).price(12.5)
                .startTime(LocalDateTime.now(ZoneId.of("Europe/Madrid")).plusDays(1)).build());
        return tickets.save(Ticket.builder().session(session)
                .row("A").seat("1").status(BuyStatus.LIBRE).build());
    }

    @Test
    void persistsEntityRelationshipsAndQueriesAcrossRepositoryCalls() {
        String suffix = unique();
        User buyer = createBuyer(suffix);
        Ticket ticket = createAvailableTicket(suffix);
        ticket.setUser(buyer);
        ticket.setStatus(BuyStatus.PAGADO);
        tickets.saveAndFlush(ticket);

        Ticket stored = tickets.findById(ticket.getId()).orElseThrow();
        assertEquals(buyer.getId(), stored.getUser().getId());
        assertEquals(12.5, stored.getSession().getPrice());
        assertEquals(1, tickets.findByUser_IdAndStatusOrderByBuyDateTimeDesc(
                buyer.getId(), BuyStatus.PAGADO).size());
        assertTrue(tickets.existsByUser_IdAndSession_Movie_IdAndStatus(
                buyer.getId(), stored.getSession().getMovie().getId(), BuyStatus.PAGADO));
    }

    @Test
    void postgresEnforcesUniqueUserEmail() {
        String suffix = unique();
        createBuyer(suffix);
        Integer count = jdbc.queryForObject(
                "select count(*) from users where email = ?", Integer.class,
                "pg_" + suffix + "@example.com");
        assertEquals(1, count);
        org.springframework.dao.DataIntegrityViolationException ex = assertThrows(
                org.springframework.dao.DataIntegrityViolationException.class,
                () -> jdbc.update(
                        "insert into users (email, username, password, role, active) values (?, ?, ?, ?, ?)",
                        "pg_" + suffix + "@example.com", "other_" + suffix,
                        "password", "ROLE_USER", true));
        assertNotNull(ex);
    }

    @Test
    void postgresRejectsDuplicateSeatWithinSameSession() {
        String suffix = unique();
        Ticket original = createAvailableTicket(suffix);
        long sessionId = original.getSession().getId();

        assertEquals(1, jdbc.queryForObject(
                "SELECT count(*) FROM tickets WHERE session_id = ? AND fila = ? AND seat = ?",
                Integer.class, sessionId, "A", "1"));

        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> jdbc.update(
                        "INSERT INTO tickets (session_id, fila, seat, status) VALUES (?, ?, ?, ?)",
                        sessionId, "A", "1", BuyStatus.LIBRE.name()));

        assertEquals(1, jdbc.queryForObject(
                "SELECT count(*) FROM tickets WHERE session_id = ? AND fila = ? AND seat = ?",
                Integer.class, sessionId, "A", "1"));
    }

    @Test
    void postgresAllowsSameSeatInDifferentSessions() {
        Ticket first = createAvailableTicket(unique());
        Ticket second = createAvailableTicket(unique());

        assertNotEquals(first.getSession().getId(), second.getSession().getId());
        assertEquals("A", first.getRow());
        assertEquals("1", first.getSeat());
        assertEquals("A", second.getRow());
        assertEquals("1", second.getSeat());
    }

    @Test
    void concurrentInventoryGenerationCreatesExactlyOneSeatBatch() throws Exception {
        String suffix = unique();
        Movie movie = movies.save(Movie.builder().title("Inventory movie " + suffix).active(true).build());
        Room room = rooms.save(Room.builder().name("Inventory room " + suffix).capacity(20).active(true).build());
        Session session = sessions.save(Session.builder().movie(movie).room(room).price(12.5).build());

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Callable<Integer> task = () -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Concurrent inventory start timed out");
                }
                Session persisted = sessions.findById(session.getId()).orElseThrow();
                return checkout.generarTickets(persisted).size();
            };
            Future<Integer> first = pool.submit(task);
            Future<Integer> second = pool.submit(task);

            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            int generated = first.get(20, TimeUnit.SECONDS) + second.get(20, TimeUnit.SECONDS);
            assertEquals(20, generated, "Only one request may create the inventory");
            assertEquals(20, tickets.findBySession_Id(session.getId()).size());
        } finally {
            start.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void pessimisticCheckoutLockAllowsExactlyOneConcurrentPurchase() throws Exception {
        String suffix = unique();
        User buyer1 = createBuyer("a" + suffix);
        User buyer2 = createBuyer("b" + suffix);
        Ticket ticket = createAvailableTicket(suffix);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = pool.submit(() -> buy(ticket.getId(), buyer1, ready, start));
            Future<Boolean> second = pool.submit(() -> buy(ticket.getId(), buyer2, ready, start));
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            int successful = (first.get(20, TimeUnit.SECONDS) ? 1 : 0)
                    + (second.get(20, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, successful, "Exactly one PostgreSQL checkout must succeed");

            Ticket persisted = tickets.findById(ticket.getId()).orElseThrow();
            assertEquals(BuyStatus.PAGADO, persisted.getStatus());
            assertTrue(persisted.getUser().getId().equals(buyer1.getId())
                    || persisted.getUser().getId().equals(buyer2.getId()));
        } finally {
            start.countDown();
            pool.shutdownNow();
        }
    }

    private boolean buy(Long id, User buyer, CountDownLatch ready,
                        CountDownLatch start) throws InterruptedException {
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrent start timed out");
        }
        try {
            checkout.processCheckout(id, buyer, 0, 0, 0, 0, 0);
            return true;
        } catch (IllegalStateException alreadySold) {
            return false;
        }
    }
}

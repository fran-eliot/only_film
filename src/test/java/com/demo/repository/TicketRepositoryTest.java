package com.demo.repository;

import com.demo.model.*;
import com.demo.model.enums.BuyStatus;
import com.demo.model.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@DisplayName("Tests para TicketRepository")
public class TicketRepositoryTest {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionRepository sessionRepository;

    private User user;
    private Session session;


    @BeforeEach
    void setUp() {
        // 1. Creamos y persistimos las dependencias necesarias
        user = User.builder().email("coder@demo.com")
                .firstName("pepe")
                .lastName("pepe")
                .username("pepe")
                .password("$2a$10$Bsw2uXDBS18RshkSkZDxOOTyWPG/KSQsjwwqXEfzusiMYT7qs528y")
                .role(Role.ROLE_USER).build();
        userRepository.save(user);

        Movie movie = Movie.builder().title("Interstellar").build();
        movieRepository.save(movie);

        session = Session.builder().movie(movie).build();
        sessionRepository.save(session);
    }

    @Test
    @DisplayName("Debería guardar un ticket y generar su ID")
    void guardarTicketTest() {
        Ticket ticket = Ticket.builder()
                .row("A")
                .seat("10")
                .price(12.50)
                .user(user)
                .session(session)
                .status(BuyStatus.LIBRE)
                .build();

        Ticket guardado = ticketRepository.save(ticket);

        assertNotNull(guardado.getId());
        assertEquals("A", guardado.getRow());
        assertEquals(BuyStatus.LIBRE, guardado.getStatus());
    }

    @Test
    @DisplayName("Debería encontrar tickets por el ID de la Sesión")
    void findBySessionIdTest() {
        // Dos entradas de la sesión consultada y otra de distinta sesión.
        Ticket t1 = Ticket.builder().row("A").seat("1").session(session).user(user).build();
        Ticket t2 = Ticket.builder().row("A").seat("2").session(session).user(user).build();
        Session otherSession = sessionRepository.save(Session.builder().movie(session.getMovie()).build());
        Ticket otherTicket = Ticket.builder().row("A").seat("3").session(otherSession).user(user).build();
        ticketRepository.saveAll(List.of(t1, t2, otherTicket));

        List<Ticket> encontrados = ticketRepository.findBySession_Id(session.getId());

        assertEquals(2, encontrados.size());
        assertEquals(java.util.Set.of(t1.getId(), t2.getId()),
                encontrados.stream().map(Ticket::getId).collect(java.util.stream.Collectors.toSet()));
        assertTrue(encontrados.stream().allMatch(t -> t.getSession().getId().equals(session.getId())));

        List<Ticket> otros = ticketRepository.findBySession_Id(otherSession.getId());
        assertEquals(1, otros.size());
        assertEquals(otherTicket.getId(), otros.getFirst().getId());
        assertTrue(ticketRepository.findBySession_Id(Long.MAX_VALUE).isEmpty());
    }

    @Test
    @DisplayName("Debería encontrar tickets por el ID del Usuario")
    void findByUserIdTest() {
        Ticket ticket = Ticket.builder().row("B").seat("5").session(session).user(user).build();
        ticketRepository.save(ticket);

        List<Ticket> ticketsUsuario = ticketRepository.findByUser_IdOrderByBuyDateTimeDesc(user.getId());

        assertFalse(ticketsUsuario.isEmpty());
        assertEquals(user.getId(), ticketsUsuario.get(0).getUser().getId());
    }

    @Test
    @DisplayName("Debería encontrar solo tickets de la película solicitada")
    void findByMovieIdTest() {
        Long movieId = session.getMovie().getId();
        Movie otherMovie = movieRepository.save(Movie.builder().title("Dune").build());
        Session otherSession = sessionRepository.save(Session.builder().movie(otherMovie).build());

        Ticket firstTicket = Ticket.builder().row("C").seat("1").session(session).user(user).build();
        Ticket secondTicket = Ticket.builder().row("C").seat("2").session(session).user(user).build();
        Ticket otherTicket = Ticket.builder().row("D").seat("1").session(otherSession).user(user).build();
        ticketRepository.saveAll(List.of(firstTicket, secondTicket, otherTicket));

        List<Ticket> ticketsPelicula = ticketRepository.findBySession_Movie_Id(movieId);
        assertEquals(2, ticketsPelicula.size());
        assertEquals(java.util.Set.of(firstTicket.getId(), secondTicket.getId()),
                ticketsPelicula.stream().map(Ticket::getId).collect(java.util.stream.Collectors.toSet()));
        assertTrue(ticketsPelicula.stream().allMatch(t -> movieId.equals(t.getSession().getMovie().getId())));

        List<Ticket> ticketsOtraPelicula = ticketRepository.findBySession_Movie_Id(otherMovie.getId());
        assertEquals(1, ticketsOtraPelicula.size());
        assertEquals(otherTicket.getId(), ticketsOtraPelicula.getFirst().getId());

        Movie withoutTickets = movieRepository.save(Movie.builder().title("Sin entradas").build());
        assertTrue(ticketRepository.findBySession_Movie_Id(withoutTickets.getId()).isEmpty());
    }

    @Test
    @DisplayName("Debería encontrar tickets por estado ordenados por fecha descendente")
    void findByStatusOrderByBuyDateTimeDescTest() {
        Ticket oldPaid = Ticket.builder()
                .row("A")
                .seat("1")
                .session(session)
                .user(user)
                .status(BuyStatus.PAGADO)
                .buyDateTime(java.time.LocalDateTime.now().minusDays(1))
                .build();

        Ticket newPaid = Ticket.builder()
                .row("A")
                .seat("2")
                .session(session)
                .user(user)
                .status(BuyStatus.PAGADO)
                .buyDateTime(java.time.LocalDateTime.now())
                .build();

        Ticket free = Ticket.builder()
                .row("A")
                .seat("3")
                .session(session)
                .user(user)
                .status(BuyStatus.LIBRE)
                .build();

        ticketRepository.saveAll(List.of(oldPaid, newPaid, free));

        List<Ticket> result = ticketRepository.findByStatusOrderByBuyDateTimeDesc(BuyStatus.PAGADO);

        assertEquals(2, result.size());
        assertEquals("2", result.get(0).getSeat());
        assertEquals("1", result.get(1).getSeat());
        assertTrue(result.stream().allMatch(ticket -> ticket.getStatus() == BuyStatus.PAGADO));
    }

    @Test
    @DisplayName("Debería encontrar solo compras pagadas del usuario en orden descendente")
    void findByUserIdAndStatusOrderByBuyDateTimeDescTest() {
        java.time.LocalDateTime earlier = java.time.LocalDateTime.of(2026, 1, 10, 11, 0);
        java.time.LocalDateTime later = java.time.LocalDateTime.of(2026, 1, 12, 18, 30);

        Ticket oldPaid = Ticket.builder()
                .row("B").seat("1").session(session).user(user)
                .status(BuyStatus.PAGADO).buyDateTime(earlier).build();
        Ticket newPaid = Ticket.builder()
                .row("B").seat("2").session(session).user(user)
                .status(BuyStatus.PAGADO).buyDateTime(later).build();
        Ticket cancelled = Ticket.builder()
                .row("B").seat("3").session(session).user(user)
                .status(BuyStatus.CANCELADO).buyDateTime(later.plusDays(1)).build();

        User otherUser = userRepository.save(User.builder()
                .email("otro-historial@demo.com").firstName("Laura").lastName("Prueba")
                .username("otra_historial")
                .password("$2a$10$Bsw2uXDBS18RshkSkZDxOOTyWPG/KSQsjwwqXEfzusiMYT7qs528y")
                .role(Role.ROLE_USER).build());
        Ticket otherUserPaid = Ticket.builder()
                .row("B").seat("4").session(session).user(otherUser)
                .status(BuyStatus.PAGADO).buyDateTime(later.plusDays(2)).build();

        // Guardar primero la compra antigua para no depender del orden de inserción.
        ticketRepository.saveAll(List.of(oldPaid, newPaid, cancelled, otherUserPaid));

        List<Ticket> result = ticketRepository
                .findByUser_IdAndStatusOrderByBuyDateTimeDesc(user.getId(), BuyStatus.PAGADO);

        assertEquals(2, result.size());
        assertEquals(newPaid.getId(), result.get(0).getId());
        assertEquals(oldPaid.getId(), result.get(1).getId());
        assertTrue(result.stream().allMatch(t -> t.getStatus() == BuyStatus.PAGADO));
        assertTrue(result.stream().allMatch(t -> user.getId().equals(t.getUser().getId())));
    }

    @Test
    @DisplayName("Debería comprobar si un usuario ya compró ticket pagado para una película")
    void existsByUserIdAndMovieIdAndStatusTest() {
        Ticket paidTicket = Ticket.builder()
                .row("C")
                .seat("1")
                .session(session)
                .user(user)
                .status(BuyStatus.PAGADO)
                .build();

        ticketRepository.save(paidTicket);

        boolean exists = ticketRepository.existsByUser_IdAndSession_Movie_IdAndStatus(
                user.getId(),
                session.getMovie().getId(),
                BuyStatus.PAGADO
        );

        assertTrue(exists);
    }

    @Test
    @DisplayName("La compra pagada pertenece únicamente al comprador y a la película correctos")
    void existsByUserIdAndMovieIdAndStatusFiltersUserAndMovie() {
        Ticket paidTicket = Ticket.builder()
                .row("E")
                .seat("1")
                .session(session)
                .user(user)
                .status(BuyStatus.PAGADO)
                .build();
        ticketRepository.save(paidTicket);

        User otherUser = userRepository.save(User.builder()
                .email("otro@demo.com")
                .firstName("Ana")
                .lastName("Test")
                .username("otra")
                .password("$2a$10$Bsw2uXDBS18RshkSkZDxOOTyWPG/KSQsjwwqXEfzusiMYT7qs528y")
                .role(Role.ROLE_USER)
                .build());
        Movie otherMovie = movieRepository.save(Movie.builder().title("Otra película").build());

        Long movieId = session.getMovie().getId();
        assertTrue(ticketRepository.existsByUser_IdAndSession_Movie_IdAndStatus(
                user.getId(), movieId, BuyStatus.PAGADO));
        assertFalse(ticketRepository.existsByUser_IdAndSession_Movie_IdAndStatus(
                otherUser.getId(), movieId, BuyStatus.PAGADO));
        assertFalse(ticketRepository.existsByUser_IdAndSession_Movie_IdAndStatus(
                user.getId(), otherMovie.getId(), BuyStatus.PAGADO));
    }

    @Test
    @DisplayName("No debería encontrar compra si el estado no coincide")
    void existsByUserIdAndMovieIdAndDifferentStatusReturnsFalseTest() {
        Ticket freeTicket = Ticket.builder()
                .row("C")
                .seat("2")
                .session(session)
                .user(user)
                .status(BuyStatus.LIBRE)
                .build();

        ticketRepository.save(freeTicket);

        boolean exists = ticketRepository.existsByUser_IdAndSession_Movie_IdAndStatus(
                user.getId(),
                session.getMovie().getId(),
                BuyStatus.PAGADO
        );

        assertFalse(exists);
    }

    @Test
    @DisplayName("Debería encontrar tickets por usuario y estado sin orden específico")
    void findByUserIdAndStatusTest() {
        Ticket paidTicket = Ticket.builder()
                .row("D")
                .seat("1")
                .session(session)
                .user(user)
                .status(BuyStatus.PAGADO)
                .build();

        ticketRepository.save(paidTicket);

        List<Ticket> result = ticketRepository.findByUser_IdAndStatus(user.getId(), BuyStatus.PAGADO);

        assertEquals(1, result.size());
        assertEquals(BuyStatus.PAGADO, result.getFirst().getStatus());
    }
}
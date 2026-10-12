package com.demo.service;

import com.demo.dto.SessionForm;
import com.demo.model.Movie;
import com.demo.model.Room;
import com.demo.model.Session;
import com.demo.model.Ticket;
import com.demo.model.enums.BuyStatus;
import java.util.List;
import java.util.Objects;
import com.demo.repository.MovieRepository;
import com.demo.repository.RoomRepository;
import com.demo.repository.SessionRepository;
import com.demo.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * One application transaction for session creation and initial seat inventory.
 * Do not split the save and ticket generation between controller calls.
 */
@Service
@RequiredArgsConstructor
public class SessionManagementService {
    private final SessionRepository sessionRepository;
    private final MovieRepository movieRepository;
    private final RoomRepository roomRepository;
    private final TicketRepository ticketRepository;
    private final TicketService ticketService;

    @Transactional
    public Session save(SessionForm form) {
        validatePrice(form.getPrice());
        validateAdMinutes(form.getAdMinutes());
        boolean creating = form.getId() == null;
        Session session = creating ? new Session() : sessionRepository.findById(form.getId()).orElseThrow();

        Movie movie = form.getMovieId() == null
                ? session.getMovie() : movieRepository.findById(form.getMovieId()).orElseThrow();
        Room room = form.getRoomId() == null
                ? session.getRoom() : roomRepository.findById(form.getRoomId()).orElseThrow();

        if (creating) {
            validateCreation(movie, room);
        } else {
            validateAndSynchronizeEdit(form, session, movie, room);
        }

        applyFields(form, session, movie, room);
        if (creating) session.setActive(true);
        Session saved = sessionRepository.save(session);
        if (creating) ticketService.generarTickets(saved);
        return saved;
    }

    private static void validatePrice(Double price) {
        if (price == null || !Double.isFinite(price) || price < 0) {
            throw new IllegalArgumentException("El precio debe ser un número no negativo");
        }
    }

    private static void validateAdMinutes(Integer adMinutes) {
        if (adMinutes != null && adMinutes < 0) {
            throw new IllegalArgumentException("Los minutos de publicidad no pueden ser negativos");
        }
    }

    private static void validateCreation(Movie movie, Room room) {
        if (movie == null || room == null) {
            throw new IllegalArgumentException("Selecciona una película y una sala");
        }
        if (!Boolean.TRUE.equals(movie.getActive()) || !Boolean.TRUE.equals(room.getActive())) {
            throw new IllegalArgumentException("La película y la sala deben estar activas");
        }
        if (room.getCapacity() == null || room.getCapacity() < 1 || room.getCapacity() > 300) {
            throw new IllegalArgumentException("Capacidad de sala inválida");
        }
    }

    private void validateAndSynchronizeEdit(SessionForm form, Session session, Movie movie, Room room) {
        // Checkout locks each ticket with PESSIMISTIC_WRITE too; retain the same lock order.
        List<Ticket> inventory = ticketRepository.findBySessionIdForUpdate(session.getId());
        boolean hasSales = inventory.stream().anyMatch(t -> t.getStatus() != BuyStatus.LIBRE);
        boolean roomChanged = !Objects.equals(idOf(session.getRoom()), idOf(room));

        if (!inventory.isEmpty() && roomChanged) {
            throw new IllegalStateException("No se puede cambiar la sala con entradas generadas");
        }
        if (hasSales && hasEssentialChange(form, session, movie)) {
            throw new IllegalStateException("No se pueden cambiar los datos de una sesión con entradas vendidas");
        }
        if (!hasSales && !Objects.equals(form.getPrice(), session.getPrice())) {
            inventory.forEach(t -> t.setPrice(form.getPrice()));
        }
    }

    private static boolean hasEssentialChange(SessionForm form, Session session, Movie movie) {
        return (form.getStartTime() != null && !Objects.equals(form.getStartTime(), session.getStartTime()))
                || !Objects.equals(form.getPrice(), session.getPrice())
                || !Objects.equals(idOf(movie), idOf(session.getMovie()))
                || !Objects.equals(form.getLanguage(), session.getLanguage())
                || !Objects.equals(form.getAdMinutes(), session.getAdMinutes());
    }

    private static void applyFields(SessionForm form, Session session, Movie movie, Room room) {
        if (form.getStartTime() != null) session.setStartTime(form.getStartTime());
        session.setPrice(form.getPrice());
        session.setLanguage(form.getLanguage());
        session.setAdMinutes(form.getAdMinutes());
        session.setMovie(movie);
        session.setRoom(room);
    }

    private static Long idOf(Movie movie) { return movie == null ? null : movie.getId(); }
    private static Long idOf(Room room) { return room == null ? null : room.getId(); }
}

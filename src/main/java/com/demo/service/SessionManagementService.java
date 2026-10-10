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
        if (form.getPrice() == null || !Double.isFinite(form.getPrice()) || form.getPrice() < 0) {
            throw new IllegalArgumentException("El precio debe ser un número no negativo");
        }
        boolean creating = form.getId() == null;
        Session session = creating ? new Session() : sessionRepository.findById(form.getId()).orElseThrow();

        Movie movie = form.getMovieId() == null
                ? session.getMovie() : movieRepository.findById(form.getMovieId()).orElseThrow();
        Room room = form.getRoomId() == null
                ? session.getRoom() : roomRepository.findById(form.getRoomId()).orElseThrow();

        if (creating && (movie == null || room == null)) {
            throw new IllegalArgumentException("Selecciona una película y una sala");
        }

        if (creating) {
            if (!Boolean.TRUE.equals(movie.getActive()) || !Boolean.TRUE.equals(room.getActive())) {
                throw new IllegalArgumentException("La película y la sala deben estar activas");
            }
            if (room.getCapacity() == null || room.getCapacity() < 1 || room.getCapacity() > 300) {
                throw new IllegalArgumentException("Capacidad de sala inválida");
            }
        } else {
            // Lock all seats before checking sales: checkout holds the same per-ticket
            // PESSIMISTIC_WRITE lock, so changes cannot race with a purchase.
            List<Ticket> inventory = ticketRepository.findBySessionIdForUpdate(session.getId());
            boolean hasInventory = !inventory.isEmpty();
            boolean hasSales = inventory.stream().anyMatch(t -> t.getStatus() != BuyStatus.LIBRE);
            boolean roomChanged = !Objects.equals(idOf(session.getRoom()), idOf(room));
            if (hasInventory && roomChanged) {
                throw new IllegalStateException("No se puede cambiar la sala con entradas generadas");
            }

            boolean essentialChange =
                    (form.getStartTime() != null && !Objects.equals(form.getStartTime(), session.getStartTime()))
                    || !Objects.equals(form.getPrice(), session.getPrice())
                    || !Objects.equals(idOf(movie), idOf(session.getMovie()))
                    || !Objects.equals(form.getLanguage(), session.getLanguage())
                    || !Objects.equals(form.getAdMinutes(), session.getAdMinutes());
            if (hasSales && essentialChange) {
                throw new IllegalStateException("No se pueden cambiar los datos de una sesión con entradas vendidas");
            }

            // The ticket catalogue shows its current face price. Keep prices for
            // unsold seats in sync when an administrator changes the session price.
            if (!hasSales && !Objects.equals(form.getPrice(), session.getPrice())) {
                inventory.forEach(t -> t.setPrice(form.getPrice()));
            }
        }

        if (form.getStartTime() != null) session.setStartTime(form.getStartTime());
        session.setPrice(form.getPrice());
        session.setLanguage(form.getLanguage());
        session.setAdMinutes(form.getAdMinutes());

        session.setMovie(movie);
        session.setRoom(room);
        if (creating) session.setActive(true);
        Session saved = sessionRepository.save(session);
        if (creating) ticketService.generarTickets(saved);
        return saved;
    }
    private static Long idOf(Movie movie) { return movie == null ? null : movie.getId(); }
    private static Long idOf(Room room) { return room == null ? null : room.getId(); }
}

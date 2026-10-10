package com.demo.service;

import com.demo.dto.SessionForm;
import com.demo.model.Movie;
import com.demo.model.Room;
import com.demo.model.Session;
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

        if (form.getStartTime() != null) session.setStartTime(form.getStartTime());
        session.setPrice(form.getPrice());
        session.setLanguage(form.getLanguage());
        session.setAdMinutes(form.getAdMinutes());

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
        } else if (!ticketRepository.findBySession_Id(session.getId()).isEmpty()
                && session.getRoom() != null && room != null
                && !session.getRoom().getId().equals(room.getId())) {
            throw new IllegalArgumentException("No se puede cambiar la sala con entradas ya generadas");
        }

        session.setMovie(movie);
        session.setRoom(room);
        if (creating) session.setActive(true);
        Session saved = sessionRepository.save(session);
        if (creating) ticketService.generarTickets(saved);
        return saved;
    }
}

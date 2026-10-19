package com.demo.service;

import com.demo.dto.DashboardSalesSummary;
import com.demo.dto.DashboardMovieRanking;
import com.demo.dto.DashboardSessionOccupancy;
import java.util.List;
import com.demo.repository.TicketRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardMetricsService {
    private final TicketRepository ticketRepository;

    /** Filtra por fecha de compra, no por fecha de proyección; el límite final es exclusivo. */
    @Transactional(readOnly = true)
    public DashboardSalesSummary salesSummary(
            LocalDateTime from, LocalDateTime until, Long movieId, Long roomId) {
        if (from == null || until == null || !from.isBefore(until)) {
            throw new IllegalArgumentException("El rango de fechas debe ser válido");
        }
        if ((movieId != null && movieId <= 0) || (roomId != null && roomId <= 0)) {
            throw new IllegalArgumentException("Los identificadores de filtro deben ser positivos");
        }
        return ticketRepository.aggregatePaidSales(from, until, movieId, roomId);
    }

    /** Calcula la ocupación de una sesión a partir de todas sus entradas. */
    @Transactional(readOnly = true)
    public DashboardSessionOccupancy sessionOccupancy(Long sessionId) {
        if (sessionId == null || sessionId <= 0) {
            throw new IllegalArgumentException("El identificador de sesión debe ser positivo");
        }
        return ticketRepository.aggregateSessionOccupancy(sessionId);
    }

    /** Clasifica películas por compras realizadas dentro del rango indicado. */
    @Transactional(readOnly = true)
    public List<DashboardMovieRanking> movieRanking(LocalDateTime from, LocalDateTime until, Long roomId) {
        if (from == null || until == null || !from.isBefore(until)) {
            throw new IllegalArgumentException("El rango de fechas debe ser válido");
        }
        if (roomId != null && roomId <= 0) {
            throw new IllegalArgumentException("El identificador de sala debe ser positivo");
        }
        return ticketRepository.rankMoviesByPaidTickets(from, until, roomId);
    }
}

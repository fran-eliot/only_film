package com.demo.service;

import com.demo.dto.DashboardSalesSummary;
import com.demo.repository.TicketRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardMetricsService {
    private final TicketRepository ticketRepository;

    /** Dates filter purchases, not projection start dates; end is exclusive. */
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
}

package com.demo.dto;

/** Ocupación de una sesión calculada a partir del inventario de entradas. */
public record DashboardSessionOccupancy(long totalSeats, long paidSeats) {
    public Double occupancyPercent() {
        return totalSeats == 0 ? null : 100.0 * paidSeats / totalSeats;
    }
}

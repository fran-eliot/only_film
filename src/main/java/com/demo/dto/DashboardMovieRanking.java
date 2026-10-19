package com.demo.dto;

/** Clasificación de películas por entradas pagadas en el período seleccionado. */
public record DashboardMovieRanking(Long movieId, String movieTitle, long paidTickets) {
}

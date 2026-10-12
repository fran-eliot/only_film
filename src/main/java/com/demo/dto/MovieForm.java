package com.demo.dto;

import com.demo.model.Movie;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MovieForm {
    private Long id;
    @NotBlank(message = "El título es obligatorio")
    private String title;
    @NotBlank(message = "El director es obligatorio")
    private String director;
    @Size(max = 600, message = "La sinopsis no puede superar 600 caracteres")
    private String sinopsis;
    private Boolean active;
    @Min(value = 60, message = "La duración debe ser de al menos 60 minutos")
    private Integer durationMinutes;
    private String genre;
    @Min(value = 1888, message = "El año de estreno no puede ser anterior a 1888")
    private Integer releaseYear;
    private String imageUrl;

    public static MovieForm from(Movie movie) {
        MovieForm form = new MovieForm();
        form.id = movie.getId();
        form.title = movie.getTitle();
        form.director = movie.getDirector();
        form.sinopsis = movie.getSinopsis();
        form.active = movie.getActive();
        form.durationMinutes = movie.getDurationMinutes();
        form.genre = movie.getGenre();
        form.releaseYear = movie.getReleaseYear();
        form.imageUrl = movie.getImageUrl();
        return form;
    }
}

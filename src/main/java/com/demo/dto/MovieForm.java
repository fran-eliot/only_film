package com.demo.dto;

import com.demo.model.Movie;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MovieForm {
    private Long id;
    private String title;
    private String director;
    private String sinopsis;
    private Boolean active;
    private Integer durationMinutes;
    private String genre;
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

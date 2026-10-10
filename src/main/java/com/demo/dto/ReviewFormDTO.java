package com.demo.dto;

import com.demo.model.Review;
import com.demo.model.Movie;
import lombok.Getter;
import lombok.Setter;

/**
 * Campos editables de una reseña. No admite autor, fecha ni entidad película.
 */
@Getter
@Setter
public class ReviewFormDTO {
    private Long id;
    private String title;
    private String description;
    private Integer rating;
    private MovieReference movie;

    public static ReviewFormDTO from(Review review) {
        ReviewFormDTO form = new ReviewFormDTO();
        form.setId(review.getId());
        form.setTitle(review.getTitle());
        form.setDescription(review.getDescription());
        form.setRating(review.getRating());
        if (review.getMovie() != null) {
            form.setMovie(new MovieReference(review.getMovie().getId(), review.getMovie().getTitle()));
        }
        return form;
    }

    public record MovieReference(Long id, String title) {}
}

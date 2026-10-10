package com.demo.dto;

import com.demo.model.Review;
import lombok.Getter;
import lombok.Setter;

/** Campos editables de una reseña; excluye autor, fecha y película. */
@Getter
@Setter
public class ReviewFormDTO {
    private Long id;
    private String title;
    private String description;
    private Integer rating;

    public static ReviewFormDTO from(Review review) {
        ReviewFormDTO form = new ReviewFormDTO();
        form.setId(review.getId());
        form.setTitle(review.getTitle());
        form.setDescription(review.getDescription());
        form.setRating(review.getRating());
        return form;
    }
}

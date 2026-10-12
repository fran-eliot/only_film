package com.demo.dto;

import com.demo.model.Review;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Campos editables de una reseña; excluye autor, fecha y película. */
@Getter
@Setter
public class ReviewFormDTO {
    private Long id;
    @NotBlank
    @Size(max = 255)
    private String title;
    @NotBlank
    @Size(max = 1000)
    private String description;
    @NotNull
    @Min(1)
    @Max(5)
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

package com.demo.controller;

import com.demo.model.enums.BuyStatus;
import com.demo.repository.TicketRepository;
import com.demo.model.Review;
import com.demo.model.User;
import com.demo.model.enums.Role;
import com.demo.repository.MovieRepository;
import com.demo.repository.ReviewRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@AllArgsConstructor
public class ReviewController {

    // inyectar el repositorio de reviews
    private final ReviewRepository reviewRepository;
    private final MovieRepository movieRepository;
    private final TicketRepository ticketRepository;

    // getmapping reviews
    @GetMapping("/reviews")
    public String reviews(Model model) {
        model.addAttribute("reviews", reviewRepository.findAllByOrderByCreationDateDesc());
        return "reviews/review-list";
    }

    @GetMapping("/reviews/{id}")
    public String reviewDetail(Model model, @PathVariable Long id,
                               @RequestParam(required = false) String returnUrl) {
        model.addAttribute("review", reviewRepository.findById(id).orElseThrow());
        // Seguridad -> solo aceptamos rutas internas (empiezan por /)
        String backUrl = (returnUrl != null && returnUrl.startsWith("/")) ? returnUrl : "/reviews";
        model.addAttribute("returnUrl", backUrl);
        return "reviews/review-detail";
    }

    @GetMapping("/reviews/new")
    public String newReview(Model model,
                            @RequestParam(required = false) Long movieId,
                            @AuthenticationPrincipal User currentUser,
                            RedirectAttributes ra) {

        if (movieId == null ||
                !ticketRepository.existsByUser_IdAndSession_Movie_IdAndStatus(currentUser.getId(), movieId, BuyStatus.PAGADO)) {
            ra.addFlashAttribute("error", "Debes haber comprado una entrada de esta película para reseñarla.");
            return movieId != null ? "redirect:/movies/" + movieId : "redirect:/movies";
        }

        Review review = new Review();
        review.setMovie(movieRepository.findById(movieId).orElseThrow());
        model.addAttribute("review", review);
        return "reviews/review-form";
    }

    @GetMapping("/reviews/edit/{id}")
    public String editReview(Model model, @PathVariable Long id,
                             @AuthenticationPrincipal User userActual) {
        Review review = reviewRepository.findById(id).orElseThrow();

        // Solo el autor o un admin puede editar
        boolean isAdmin = userActual.getRole().equals(Role.ROLE_ADMIN);
        boolean isOwner = review.getUser() != null
                && review.getUser().getUsername().equals(userActual.getUsername());

        if (!isAdmin && !isOwner)
            return "redirect:/reviews/" + id;

        model.addAttribute("review", review);
        return "reviews/review-form";
    }

    //movieId viene del campo hidden del formulario para asociar la película correctamente
    @PostMapping("/reviews")
    public String saveReview(@ModelAttribute Review review,
                             @RequestParam(required = false) Long movieId,
                             @AuthenticationPrincipal User currentUser,
                             RedirectAttributes ra) {

        Review persisted;

        if (review.getId() != null) {
            // Never trust ownership, movie or timestamp supplied by form fields.
            persisted = reviewRepository.findById(review.getId()).orElseThrow();
            boolean isAdmin = currentUser.getRole() == Role.ROLE_ADMIN;
            boolean isOwner = persisted.getUser() != null
                    && persisted.getUser().getId().equals(currentUser.getId());
            if (!isAdmin && !isOwner) {
                throw new AccessDeniedException("No puedes modificar esta reseña");
            }

            // Only editable fields are copied: the original author, movie and date are immutable.
            persisted.setTitle(review.getTitle());
            persisted.setDescription(review.getDescription());
            persisted.setRating(review.getRating());
        } else {
            // Creation requires an actual paid ticket for the target movie.
            if (movieId == null ||
                    !ticketRepository.existsByUser_IdAndSession_Movie_IdAndStatus(
                            currentUser.getId(), movieId, BuyStatus.PAGADO)) {
                ra.addFlashAttribute("error", "Debes haber comprado una entrada de esta película para reseñarla.");
                return movieId != null ? "redirect:/movies/" + movieId : "redirect:/movies";
            }

            persisted = new Review();
            persisted.setTitle(review.getTitle());
            persisted.setDescription(review.getDescription());
            persisted.setRating(review.getRating());
            persisted.setMovie(movieRepository.findById(movieId).orElseThrow());
            persisted.setUser(currentUser);
        }

        reviewRepository.save(persisted);

        if (persisted.getMovie() != null)
            return "redirect:/movies/" + persisted.getMovie().getId() + "#reviews";
        return "redirect:/reviews";
    }

    @PostMapping("/reviews/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        // Solo ADMIN pasa por aqui, comprobado con debug, SecurityConfig ya lo garantiza a nivel de ruta
        reviewRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("message", "Reseña eliminada correctamente.");
        return "redirect:/reviews";
    }
}
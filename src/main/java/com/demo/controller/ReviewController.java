package com.demo.controller;

import com.demo.dto.ReviewFormDTO;
import com.demo.model.enums.BuyStatus;
import com.demo.repository.TicketRepository;
import com.demo.model.Review;
import com.demo.model.User;
import com.demo.model.enums.Role;
import com.demo.repository.MovieRepository;
import com.demo.repository.ReviewRepository;
import lombok.AllArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@AllArgsConstructor
public class ReviewController {
    private static final String REDIRECT_MOVIE_DETAIL = "redirect:/movies/";
    private static final String MODEL_REVIEW = "review";
    private static final String MODEL_MOVIE = "movie";
    private static final String REVIEW_FORM_VIEW = "reviews/review-form";


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
        model.addAttribute(MODEL_REVIEW, reviewRepository.findById(id).orElseThrow());
        // Seguridad -> solo aceptamos rutas internas (empiezan por /)
        String backUrl = (returnUrl != null && returnUrl.startsWith("/")
                && !returnUrl.startsWith("//") && !returnUrl.contains("\\")) ? returnUrl : "/reviews";
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
            return movieId != null ? REDIRECT_MOVIE_DETAIL + movieId : "redirect:/movies";
        }

        model.addAttribute(MODEL_REVIEW, new ReviewFormDTO());
        model.addAttribute(MODEL_MOVIE, movieRepository.findById(movieId).orElseThrow());
        return REVIEW_FORM_VIEW;
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

        model.addAttribute(MODEL_REVIEW, ReviewFormDTO.from(review));
        model.addAttribute(MODEL_MOVIE, review.getMovie());
        return REVIEW_FORM_VIEW;
    }

    //movieId viene del campo hidden del formulario para asociar la película correctamente
    @PostMapping("/reviews")
    public String saveReview(@Valid @ModelAttribute("review") ReviewFormDTO review,
                             BindingResult bindingResult,
                             @RequestParam(required = false) Long movieId,
                             @AuthenticationPrincipal User currentUser,
                             RedirectAttributes ra,
                             Model model) {

        if (review.getId() != null) {
            return saveExistingReview(review, bindingResult, currentUser, model);
        }
        return saveNewReview(review, bindingResult, movieId, currentUser, ra, model);
    }

    private String saveExistingReview(ReviewFormDTO form, BindingResult bindingResult,
                                      User currentUser, Model model) {
        // Comprobar permisos antes de procesar errores de validación del formulario.
        Review persisted = reviewRepository.findById(form.getId()).orElseThrow();
        verifyEditPermission(persisted, currentUser);

        if (bindingResult.hasErrors()) {
            model.addAttribute(MODEL_MOVIE, persisted.getMovie());
            return REVIEW_FORM_VIEW;
        }

        // La película, el autor y la fecha de creación nunca se modifican desde el formulario.
        persisted.setTitle(form.getTitle());
        persisted.setDescription(form.getDescription());
        persisted.setRating(form.getRating());
        return persistAndRedirect(persisted);
    }

    private String saveNewReview(ReviewFormDTO form, BindingResult bindingResult,
                                 Long movieId, User currentUser,
                                 RedirectAttributes ra, Model model) {
        // La creación requiere una entrada pagada para esta película.
        if (movieId == null ||
                !ticketRepository.existsByUser_IdAndSession_Movie_IdAndStatus(
                        currentUser.getId(), movieId, BuyStatus.PAGADO)) {
            ra.addFlashAttribute("error", "Debes haber comprado una entrada de esta película para reseñarla.");
            return movieId != null ? REDIRECT_MOVIE_DETAIL + movieId : "redirect:/movies";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute(MODEL_MOVIE, movieRepository.findById(movieId).orElseThrow());
            return REVIEW_FORM_VIEW;
        }

        Review persisted = new Review();
        persisted.setTitle(form.getTitle());
        persisted.setDescription(form.getDescription());
        persisted.setRating(form.getRating());
        persisted.setMovie(movieRepository.findById(movieId).orElseThrow());
        persisted.setUser(currentUser);
        return persistAndRedirect(persisted);
    }

    private String persistAndRedirect(Review review) {
        reviewRepository.save(review);
        if (review.getMovie() != null) {
            return REDIRECT_MOVIE_DETAIL + review.getMovie().getId() + "#reviews";
        }
        return "redirect:/reviews";
    }

    private static void verifyEditPermission(Review persisted, User currentUser) {
        boolean isAdmin = currentUser.getRole() == Role.ROLE_ADMIN;
        boolean isOwner = persisted.getUser() != null
                && persisted.getUser().getId().equals(currentUser.getId());
        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("No puedes modificar esta reseña");
        }
    }

    @PostMapping("/reviews/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        // Solo ADMIN pasa por aqui, comprobado con debug, SecurityConfig ya lo garantiza a nivel de ruta
        reviewRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("message", "Reseña eliminada correctamente.");
        return "redirect:/reviews";
    }
}
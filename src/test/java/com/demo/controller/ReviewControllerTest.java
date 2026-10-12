package com.demo.controller;

import com.demo.dto.ReviewFormDTO;
import com.demo.model.Session;
import com.demo.model.Ticket;
import com.demo.model.enums.BuyStatus;
import com.demo.repository.SessionRepository;
import com.demo.repository.TicketRepository;
import com.demo.model.Movie;
import com.demo.model.Review;
import com.demo.model.User;
import com.demo.model.enums.Role;
import com.demo.repository.MovieRepository;
import com.demo.repository.ReviewRepository;
import com.demo.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired private SessionRepository sessionRepository;
    @Autowired private TicketRepository ticketRepository;

    private Movie movie;
    private User user;
    private User otherUser;
    private Review review;
    private User admin;

    @BeforeEach
    void setUp() {

        reviewRepository.deleteAll();
        movieRepository.deleteAll();
        userRepository.deleteAll();

        movie = movieRepository.save(
                Movie.builder()
                        .title("Interstellar")
                        .active(true)
                        .build()
        );

        user = userRepository.save(
                User.builder()
                        .username("user_test")
                        .email("user@test.com")
                        .firstName("Juan")
                        .password(passwordEncoder.encode("1234"))
                        .role(Role.ROLE_USER)
                        .build()
        );

        Session session = sessionRepository.save(
                Session.builder().movie(movie).price(10.0).build());

        ticketRepository.save(
                Ticket.builder().user(user).session(session).status(BuyStatus.PAGADO).build());

        otherUser = userRepository.save(
                User.builder()
                        .username("other_test")
                        .email("other@test.com")
                        .firstName("Pedro")
                        .password(passwordEncoder.encode("1234"))
                        .role(Role.ROLE_USER)
                        .build()
        );

        admin = userRepository.save(
                User.builder()
                        .username("admin")
                        .email("admin@test.com")
                        .firstName("Admin")
                        .password(passwordEncoder.encode("1234"))
                        .role(Role.ROLE_ADMIN)
                        .build()
        );

        review = reviewRepository.save(
                Review.builder()
                        .title("Gran película")
                        .description("Muy recomendable")
                        .rating(5)
                        .movie(movie)
                        .user(user)
                        .build()
        );
    }

    // Verificamos que GET /reviews devuelve el listado de reviews
    @Test
    void reviews_shouldReturnReviewListView() throws Exception {

        mockMvc.perform(get("/reviews").with(user(user)))
                .andExpect(status().isOk())
                .andExpect(view().name("reviews/review-list"))
                .andExpect(model().attributeExists("reviews"));
    }

    // Verificamos que GET /reviews/{id} carga el detalle correctamente
    @Test
    void reviewDetail_shouldReturnDetailView() throws Exception {

        mockMvc.perform(get("/reviews/" + review.getId()).with(user(user)))
                .andExpect(status().isOk())
                .andExpect(view().name("reviews/review-detail"))
                .andExpect(model().attributeExists("review"));
    }

    // Verificamos que GET /reviews/new devuelve el formulario vacío
    @Test
    void newReview_shouldReturnForm() throws Exception {

        mockMvc.perform(get("/reviews/new")
                        .param("movieId", movie.getId().toString())
                        .with(user(user)))
                .andExpect(status().isOk())
                .andExpect(view().name("reviews/review-form"))
                .andExpect(model().attributeExists("review", "movie"))
                .andExpect(model().attribute("review", org.hamcrest.Matchers.instanceOf(ReviewFormDTO.class)));
    }

    @Test
    void reviewFormRendersPageTitleFromSharedHeadFragment() throws Exception {
        mockMvc.perform(get("/reviews/new")
                        .param("movieId", movie.getId().toString())
                        .with(user(user)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "<title>Formulario de reseña — Only Film</title>")));
    }

    @Test
    void reviewFormRendersAccessibleRatingButtonsAndNavigation() throws Exception {
        mockMvc.perform(get("/reviews/new")
                        .param("movieId", movie.getId().toString())
                        .with(user(user)))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("aria-label=\"Navegación principal\"")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("<fieldset class=\"mb-4\"")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("<legend class=\"form-label fw-semibold\"")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("role=\"button\""))))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("aria-label=\"5 estrellas\"")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("aria-label=\"2 estrellas\"")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString("aria-pressed=\"false\"")));
    }

    @Test
    void navigationAvatarUsesCssDimensionsInsteadOfDeprecatedHtmlAttributes() throws Exception {
        mockMvc.perform(get("/reviews/new")
                        .param("movieId", movie.getId().toString())
                        .with(user(user)))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.containsString(
                                "width: 36px; height: 36px; border-color:")));
    }

    // Verificamos que POST /reviews guarda una nueva review en BD
    @Test
    void saveReview_shouldCreateReview() throws Exception {

        long now = reviewRepository.count();

        mockMvc.perform(post("/reviews")
                        .with(user(user)).with(csrf())
                        .param("title", "Nueva review")
                        .param("description", "Muy buena")
                        .param("rating", "4")
                        .param("movieId", movie.getId().toString()))
                .andExpect(status().is3xxRedirection());

        long updated = reviewRepository.count();

        assertEquals(now + 1, updated);
    }

    // Verificamos que el autor de la review puede editarla
    @Test
    void editReview_shouldAllowOwner() throws Exception {

        mockMvc.perform(get("/reviews/edit/" + review.getId())
                        .with(user(user)))
                .andExpect(status().isOk())
                .andExpect(view().name("reviews/review-form"))
                .andExpect(model().attribute("review", org.hamcrest.Matchers.instanceOf(ReviewFormDTO.class)));
    }

    // Verificamos que un usuario que no es el autor es redirigido
    @Test
    void editReview_shouldRedirectNonOwner() throws Exception {

        mockMvc.perform(get("/reviews/edit/" + review.getId())
                        .with(user(otherUser)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reviews/" + review.getId()));
    }

    // Verificamos que un admin puede eliminar una review
    @Test
    void deleteReview_shouldDeleteReview() throws Exception {

        Long id = review.getId();

        mockMvc.perform(post("/reviews/delete/" + id)
                        .with(user(admin)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reviews"));

        assertThat(reviewRepository.findById(id)).isEmpty();
    }

    @Test
    void saveReview_shouldRejectWithoutTicket() throws Exception {
        long now = reviewRepository.count();

        mockMvc.perform(post("/reviews")
                        .with(user(otherUser)).with(csrf())
                        .param("title", "X").param("description", "Y").param("rating", "3")
                        .param("movieId", movie.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/movies/" + movie.getId()));

        assertEquals(now, reviewRepository.count()); // NO se creó la reseña
    }



    @Test
    void saveReview_shouldRejectDirectPostByNonOwnerAndKeepOriginal() throws Exception {
        mockMvc.perform(post("/reviews").with(user(otherUser)).with(csrf())
                        .param("id", review.getId().toString())
                        .param("movieId", movie.getId().toString())
                        .param("title", "Reseña robada")
                        .param("description", "Contenido modificado")
                        .param("rating", "1"))
                .andExpect(status().isForbidden());

        Review persisted = reviewRepository.findById(review.getId()).orElseThrow();
        assertEquals("Gran película", persisted.getTitle());
        assertEquals(5, persisted.getRating());
        assertEquals(user.getId(), persisted.getUser().getId());
    }

    @Test
    void saveReview_shouldAllowOwnerWithoutChangingMovieOrAuthor() throws Exception {
        Movie anotherMovie = movieRepository.save(Movie.builder().title("Otra película").active(true).build());
        var originalCreationDate = review.getCreationDate();

        mockMvc.perform(post("/reviews").with(user(user)).with(csrf())
                        .param("id", review.getId().toString())
                        .param("movieId", anotherMovie.getId().toString())
                        .param("user.id", otherUser.getId().toString())
                        .param("creationDate", "2000-01-01T00:00:00")
                        .param("title", "Actualizada por autor")
                        .param("description", "Nueva opinión")
                        .param("rating", "4"))
                .andExpect(status().is3xxRedirection());

        Review persisted = reviewRepository.findById(review.getId()).orElseThrow();
        assertEquals("Actualizada por autor", persisted.getTitle());
        assertEquals(4, persisted.getRating());
        assertEquals(movie.getId(), persisted.getMovie().getId());
        assertEquals(user.getId(), persisted.getUser().getId());
        assertEquals(originalCreationDate, persisted.getCreationDate());
    }

    @Test
    void saveReview_shouldAllowAdminToEditWithoutChangingAuthor() throws Exception {
        mockMvc.perform(post("/reviews").with(user(admin)).with(csrf())
                        .param("id", review.getId().toString())
                        .param("movieId", movie.getId().toString())
                        .param("title", "Corregida por admin")
                        .param("description", "Moderación")
                        .param("rating", "3"))
                .andExpect(status().is3xxRedirection());

        Review persisted = reviewRepository.findById(review.getId()).orElseThrow();
        assertEquals("Corregida por admin", persisted.getTitle());
        assertEquals(user.getId(), persisted.getUser().getId());
    }

    @Test
    void saveReview_shouldIgnoreInjectedIdentityAndMovieOnCreate() throws Exception {
        mockMvc.perform(post("/reviews").with(user(user)).with(csrf())
                        .param("movieId", movie.getId().toString())
                        .param("user.id", otherUser.getId().toString())
                        .param("creationDate", "2000-01-01T00:00:00")
                        .param("title", "Nueva legítima")
                        .param("description", "Una reseña")
                        .param("rating", "5"))
                .andExpect(status().is3xxRedirection());

        Review created = reviewRepository.findAll().stream()
                .filter(r -> "Nueva legítima".equals(r.getTitle())).findFirst().orElseThrow();
        assertEquals(user.getId(), created.getUser().getId());
        assertEquals(movie.getId(), created.getMovie().getId());
        assertThat(created.getCreationDate()).isAfter(java.time.LocalDateTime.of(2020, 1, 1, 0, 0));
    }

    @Test
    void reviewDetail_shouldPreserveSafeInternalReturnUrl() throws Exception {
        mockMvc.perform(get("/reviews/" + review.getId())
                        .param("returnUrl", "/movies/" + movie.getId())
                        .with(user(user)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("returnUrl", "/movies/" + movie.getId()));
    }

    @Test
    void reviewDetail_shouldRejectExternalReturnUrls() throws Exception {
        for (String unsafe : new String[] {
                "//external.example/path",
                "/\\\\external.example/path",
                "https://external.example/path",
                "relative/path"
        }) {
            mockMvc.perform(get("/reviews/" + review.getId())
                            .param("returnUrl", unsafe)
                            .with(user(user)))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("returnUrl", "/reviews"));
        }
    }
    @Test
    void saveReview_shouldRejectInvalidNewReviewWithoutPersisting() throws Exception {
        long initialCount = reviewRepository.count();
        for (String[] invalid : new String[][] {
                {"0", "Título", "Descripción"},
                {"6", "Título", "Descripción"},
                {"", "Título", "Descripción"},
                {"3", " ", "Descripción"},
                {"3", "Título", " "},
                {"3", "X".repeat(256), "Descripción"},
                {"3", "Título", "X".repeat(1001)}
        }) {
            mockMvc.perform(post("/reviews").with(user(user)).with(csrf())
                            .param("movieId", movie.getId().toString())
                            .param("rating", invalid[0])
                            .param("title", invalid[1])
                            .param("description", invalid[2]))
                    .andExpect(status().isBadRequest());
            assertEquals(initialCount, reviewRepository.count());
        }
    }

    @Test
    void saveReview_shouldRejectInvalidEditAndPreserveOriginal() throws Exception {
        mockMvc.perform(post("/reviews").with(user(user)).with(csrf())
                        .param("id", review.getId().toString())
                        .param("rating", "9")
                        .param("title", "Título alterado")
                        .param("description", "Descripción alterada"))
                .andExpect(status().isBadRequest());

        Review persisted = reviewRepository.findById(review.getId()).orElseThrow();
        assertEquals("Gran película", persisted.getTitle());
        assertEquals("Muy recomendable", persisted.getDescription());
        assertEquals(5, persisted.getRating());
    }
}

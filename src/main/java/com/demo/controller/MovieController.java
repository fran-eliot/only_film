package com.demo.controller;

import com.demo.model.Movie;
import com.demo.model.User;
import com.demo.model.enums.BuyStatus;
import com.demo.repository.MovieRepository;
import com.demo.repository.ReviewRepository;
import com.demo.repository.SessionRepository;
import com.demo.repository.TicketRepository;
import com.demo.service.FileService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;


@Controller
@AllArgsConstructor
public class MovieController {
    //Inyectar el repositorio de movie
    private final MovieRepository movieRepository;
    private final FileService fileService;
    private final SessionRepository sessionRepository;
    private final ReviewRepository reviewRepository;
    private final TicketRepository ticketRepository;

    //Redirect raíz a /movies
    @GetMapping("/")
    public String home() {
        return "redirect:/movies";
    }

    //GetMapping de peliculas
    @GetMapping("movies")
    public String moviesList(Model model,
                             @RequestParam(required = false) Integer durationMinutes,
                             @RequestParam(required = false) String genre,
                             @RequestParam(required = false) Integer releaseYear,
                             @RequestParam(required = false) String title
    ){
        // Bug encontrado, solucion -> Convertir strings vacíos a null para que el JPQL los trate como "sin filtro"
        if (genre != null && genre.isBlank()) genre = null;
        if (title != null && title.isBlank()) title = null;

        List<Movie> movies = movieRepository.findActiveFiltering(durationMinutes, genre, releaseYear, title);
        model.addAttribute("movies", movies);
        model.addAttribute("numMovies", movies.size());
        model.addAttribute("title", "Listado de películas");
        return "movies/movie-list";
    }

    //Ver las asociaciones que apuntan a movie (session y review)
    @GetMapping("movies/{id}")
    public String movieDetail(@PathVariable Long id, Model model, @AuthenticationPrincipal User user){
        Optional<Movie> movieOptional = movieRepository.findById(id);
        if(movieOptional.isPresent()){
            Movie movie = movieOptional.get();
            model.addAttribute("movie", movie);
            model.addAttribute("sessions", sessionRepository.findByMovie_IdAndActiveTrueOrderByStartTimeAsc(id));
            model.addAttribute("reviews", reviewRepository.findByMovieIdOrderByCreationDateDesc(id));
            model.addAttribute("esLocale", new java.util.Locale("es"));     // Lo puse en el HTML pero se queja, la solucion: sacarlo fuera al controlador y pasarle el atributo

            boolean canReview = user != null &&
                    ticketRepository.existsByUser_IdAndSession_Movie_IdAndStatus(user.getId(), id, BuyStatus.PAGADO);
            model.addAttribute("canReview", canReview);

            return "movies/movie-detail";
        }
        return"redirect:/movies";

    }
    //Desactivar una pelicula
    @PostMapping("movies/deactivate/{id}")
    public String movieDeactivate(@PathVariable Long id, Model model){
        Optional<Movie> movieOptional = movieRepository.findById(id);

        if (movieOptional.isPresent()) {
            Movie movieDeactivate = movieOptional.get();
            movieDeactivate.setActive(false);
            movieRepository.save(movieDeactivate);
        }
        return "redirect:/movies";
    }

    //Creamos una pelicula
    @GetMapping("movies/new")
    public String newMovie(Model model){
        model.addAttribute("movie", new Movie());
        return "movies/movie-form";
    }

    //Editamos una pelicula existente
    @GetMapping("movies/edit/{id}")
    public String editMovie(@PathVariable Long id, Model model){
        //Se pude hacer tambien con el Optional
        model.addAttribute("movie", movieRepository.findById(id).orElseThrow());
        return "movies/movie-form";
    }

    @PostMapping("movies")
    public String createMovie(@ModelAttribute Movie movie,
                              @RequestParam("imageFile") MultipartFile imageFile){
        String imageUrl = fileService.store(imageFile);
        if (imageUrl != null) {
            movie.setImageUrl(imageUrl);
        }

        movieRepository.save(movie);
        return "redirect:/movies/" + movie.getId();
    }


}

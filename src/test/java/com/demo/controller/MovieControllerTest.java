package com.demo.controller;

import com.demo.model.Movie;
import com.demo.repository.MovieRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
public class MovieControllerTest {
    @Autowired //Se agrega una anotacion por cada repository que necesitemos aqui
    MovieRepository movieRepository;
    @Autowired
    MockMvc mockMvc;
    Movie movieToDeactivate;

    @BeforeEach
    void setUp(){
        movieRepository.deleteAll();
        movieRepository.saveAll(List.of(
                Movie.builder().active(true).title("The Dark Knight").genre("Action").durationMinutes(152).releaseYear(2008).director("Christopher Nolan").build(),
                Movie.builder().active(true).title("Inception").genre("Sci-Fi").durationMinutes(148).releaseYear(2010).director("Christopher Nolan").build(),
                Movie.builder().active(true).title("The Dark Knight Rises").genre("Action").durationMinutes(164).releaseYear(2012).director("Christopher Nolan").build(),
                Movie.builder().active(true).title("Interstellar").genre("Sci-Fi").durationMinutes(169).releaseYear(2014).director("Christopher Nolan").build(),
                Movie.builder().active(true).title("The Avengers").genre("Action").durationMinutes(143).releaseYear(2012).director("Joss Whedon").build(),
                Movie.builder().active(true).title("Into the Dark").genre("Drama").durationMinutes(132).releaseYear(2019).director("Blumhouse").build()
        ));

        movieToDeactivate = movieRepository.save(
        Movie.builder().active(true).title("El bar de Moe").build()
        );
    }
//Este no falla porque agregamos el imageFile en el Controller, solucionar
    @Test
    void createNewMovie() throws Exception{
        //Contamos la cantidad de peliculas ACTUAL
        long now = movieRepository.count();

        //Con mockMvc enviamos la pelicula nueva al controller
        mockMvc.perform(multipart("/movies").file("imageFile", new byte[0])
                .param("title", "TitleMovie Test")
                .param("director","DirectorMovie Test"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/movies/*"));

        //Contamos la cantidad de peliculas ACTUALIZADO
        long update = movieRepository.count();

        assertEquals(now + 1, update);
    }


    @Test
    void invalidCreationPreservesFieldsAndDoesNotPersist() throws Exception {
        long before = movieRepository.count();
        mockMvc.perform(multipart("/movies").file("imageFile", new byte[0])
                        .param("title", " ")
                        .param("director", "Director de prueba")
                        .param("durationMinutes", "59")
                        .param("releaseYear", "1887")
                        .param("sinopsis", "Sinopsis conservada"))
                .andExpect(status().isOk())
                .andExpect(view().name("movies/movie-form"))
                .andExpect(model().attributeHasFieldErrors("movie", "title", "durationMinutes", "releaseYear"))
                .andExpect(model().attribute("movie", hasProperty("director", is("Director de prueba"))))
                .andExpect(content().string(containsString("Sinopsis conservada")))
                .andExpect(content().string(containsString("titleError")));
        assertEquals(before, movieRepository.count());
    }

    @Test
    void invalidEditKeepsOriginalAndStoredPoster() throws Exception {
        Movie original = movieRepository.findAll().getFirst();
        original.setImageUrl("/uploads/previous.png");
        movieRepository.save(original);
        long before = movieRepository.count();
        mockMvc.perform(multipart("/movies").file("imageFile", new byte[0])
                        .param("id", original.getId().toString())
                        .param("title", "Nuevo título")
                        .param("director", " ")
                        .param("durationMinutes", "120"))
                .andExpect(status().isOk())
                .andExpect(view().name("movies/movie-form"))
                .andExpect(model().attributeHasFieldErrors("movie", "director"))
                .andExpect(model().attribute("movie", hasProperty("imageUrl", is("/uploads/previous.png"))))
                .andExpect(content().string(containsString("Nuevo título")));
        assertEquals(before, movieRepository.count());
        assertEquals(original.getTitle(), movieRepository.findById(original.getId()).orElseThrow().getTitle());
    }

    @Test
    void oversizedSynopsisIsRejectedBeforePersistence() throws Exception {
        long before = movieRepository.count();
        mockMvc.perform(multipart("/movies").file("imageFile", new byte[0])
                        .param("title", "Título válido")
                        .param("director", "Director válido")
                        .param("sinopsis", "a".repeat(601)))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("movie", "sinopsis"));
        assertEquals(before, movieRepository.count());
    }

    @Test
    void invalidUploadDoesNotDiscardEditFieldsOrChangeMovie() throws Exception {
        Movie original = movieRepository.findAll().getFirst();
        original.setImageUrl("/uploads/previous.png");
        movieRepository.save(original);
        mockMvc.perform(multipart("/movies")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "imageFile", "invalid.txt", "text/plain", "not an image".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                        .param("id", original.getId().toString())
                        .param("title", "Título editado")
                        .param("director", "Director editado"))
                .andExpect(status().isOk())
                .andExpect(view().name("movies/movie-form"))
                .andExpect(model().attributeExists("imageError"))
                .andExpect(model().attribute("movie", hasProperty("title", is("Título editado"))))
                .andExpect(model().attribute("movie", hasProperty("imageUrl", is("/uploads/previous.png"))))
                .andExpect(content().string(containsString("imageFileError")));
        assertEquals(original.getTitle(), movieRepository.findById(original.getId()).orElseThrow().getTitle());
    }

    @Test
    void deactivateMovie() throws Exception{
        assertTrue(movieToDeactivate.getActive());

        Long id = movieToDeactivate.getId();

        mockMvc.perform(post("/movies/deactivate/" + id))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/movies"));
        //Traemos una movie de BD para comprobar que el estado de active es false
        Movie movieDB = movieRepository.findById(id).orElseThrow();
        assertFalse(movieDB.getActive());
    }

    //Las peliculas activas segun setUp son 7
    @Test
    void movieListRendersPageTitleFromSharedHeadFragment() throws Exception {
        mockMvc.perform(get("/movies"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<title>Cartelera — Only Film</title>")));
    }

    @Test
    void moviesFull() throws Exception {
        //Se invoca EndPoint http://localhost:8080/movies, se lanza peticion HTTP Get al controles / movies
        //Luego verificamos que devuelve el status 200, que la vista es movies/movie-list
        mockMvc.perform(get("/movies"))
                .andExpect(status().isOk())
                .andExpect(view().name("movies/movie-list"))
                .andExpect(model().attributeExists("movies"))
                .andExpect(model().attribute("movies", hasSize(7)));
    }

    @Test
    void movieListHasAccessibleSearchFiltersAndPosterDescriptions() throws Exception {
        mockMvc.perform(get("/movies"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "for=\"movieTitleFilter\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "for=\"movieYearFilter\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "for=\"movieDurationFilter\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "Cartel de The Dark Knight")));
    }

    @Test
    void movieEmpty() throws Exception{
        movieRepository.deleteAll();
        mockMvc.perform(get("/movies"))
                .andExpect(status().isOk())
                .andExpect(view().name("movies/movie-list"))
                .andExpect(model().attribute("movies", hasSize(0)));
    }

    @Test
    void filterMoviesByTitle() throws Exception{
        mockMvc.perform(get("/movies").param("title", "dark"))
                .andExpect(model().attribute("movies", hasSize(3)));
    }

    @Test
    void filterMoviesByGenre() throws Exception{
        mockMvc.perform(get("/movies").param("genre", "Action"))
                .andExpect(model().attribute("movies", hasSize(3)));
    }

    @Test
    void filterMoviesByDuration() throws Exception{
        mockMvc.perform(get("/movies").param("durationMinutes", "150"))
                .andExpect(model().attribute("movies", hasSize(3)));
    }

    @Test
    void filterMoviesByTitleAndGenre() throws Exception{
        mockMvc.perform(get("/movies").param("title", "dark").param("genre", "Action"))
                .andExpect(model().attribute("movies", hasSize(2)));
    }

    @Test
    void movieDetailIsPresentTrue() throws Exception{
        Movie movie = movieRepository.findAll().getFirst();
        Long movieId = movie.getId();

        mockMvc.perform(get("/movies/" + movieId))
                .andExpect(status().isOk())
                .andExpect(view().name("movies/movie-detail"))
                .andExpect(model().attributeExists("movie"))
                .andExpect(model().attribute("movie", hasProperty("id", is(movieId))))
                .andExpect(model().attribute("movie", hasProperty("title", is(movie.getTitle()))));
    }

    @Test
    void movieDetailIsPresentFalse() throws Exception {
        Long idInexistente = 99999L;

        mockMvc.perform(get("/movies/" + idInexistente))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/movies"));
    }

    @Test
    void newMovieForm() throws Exception {
        mockMvc.perform(get("/movies/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("movies/movie-form"))
                .andExpect(model().attributeExists("movie"));
    }

//El enpoint /movies/edit necesita admin. ANTES de editar
    @Test
    void editMovieForm() throws Exception {
        // Verificamos que GET /movies/edit/{id} devuelve el formulario con la película CARGADA
        Movie movie = movieRepository.findAll().getFirst();
        Long movieId = movie.getId();

        mockMvc.perform(get("/movies/edit/" + movieId)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("movies/movie-form"))
                .andExpect(model().attributeExists("movie"))
                .andExpect(model().attribute("movie", hasProperty("id", is(movieId))))
                .andExpect(model().attribute("movie", hasProperty("title", is(movie.getTitle()))));
    }
//DESPUES de editar
    @Test
    void editMovie() throws Exception {
        Movie movie = movieRepository.findAll().getFirst();
        Long id = movie.getId();
        long countBefore = movieRepository.count();

        mockMvc.perform(multipart("/movies").file("imageFile", new byte[0])
                        .param("id", id.toString())
                        .param("title", "Titulo Editado")
                        .param("director", "Director Editado")
                        .param("genre", "Drama"))
                .andExpect(status().is3xxRedirection());

        assertEquals(countBefore, movieRepository.count());   // NO se creó una peli nueva
        Movie editada = movieRepository.findById(id).orElseThrow();
        assertEquals("Titulo Editado", editada.getTitle());
        assertEquals("Director Editado", editada.getDirector());
    }

    @Test
    void deactivateMovieNotFound() throws Exception {
        // Verificamos la rama del if cuando la película NO EXISTE, redirigimos a /movies
        mockMvc.perform(post("/movies/deactivate/99999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/movies"));
    }

    @Test
    void movieDetailWithSessionsAndReviews() throws Exception {
        Movie movie = movieRepository.findAll().getFirst();

        mockMvc.perform(get("/movies/" + movie.getId()))
                .andExpect(status().isOk()) // codigo 200
                .andExpect(model().attributeExists("sessions"))   // se agregan las sessions
                .andExpect(model().attributeExists("reviews"));    // y también "reviews"
    }

    @Test
    void editingMovieIgnoresForgedImageUrlAndKeepsExistingState() throws Exception {
        Movie movie = movieRepository.findAll().getFirst();
        movie.setImageUrl("/uploads/original.jpg");
        movie.setActive(true);
        movieRepository.save(movie);
        mockMvc.perform(multipart("/movies").file("imageFile", new byte[0])
                        .param("id", movie.getId().toString())
                        .param("title", "Changed title")
                        .param("imageUrl", "/uploads/forged.html"))
                .andExpect(status().is3xxRedirection());
        Movie persisted = movieRepository.findById(movie.getId()).orElseThrow();
        assertEquals("/uploads/original.jpg", persisted.getImageUrl());
        assertTrue(persisted.getActive());
        assertEquals("Changed title", persisted.getTitle());
    }

}

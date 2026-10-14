package com.demo.repository;

import com.demo.model.Movie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
@DataJpaTest
@DisplayName("Peliculas Respositorio operaciones de DB")
public class MovieRepositoryTest {
    @Autowired
    MovieRepository movieRepository;

    @Test
    void guardarPelicula(){
        //Insert into movies
        Movie movie = new Movie();
        movie.setTitle("The Age of Adaline");
        movie.setDirector("Lee Toland Krieger");
        movie.setDurationMinutes(112);
        movie.setGenre("Romance");

        movie = movieRepository.save(movie);

        assertNotNull(movie.getId());
        assertEquals("The Age of Adaline", movie.getTitle());
    }

    @Test
    void buscarMovies_vacio(){
        //Traemos todos las peliculas de BD
        List<Movie> movies = movieRepository.findAll();
        assertNotNull(movies);
        System.out.println(movies);

        assertEquals(0, movies.size());
        assertTrue(movies.isEmpty());

    }

    @Test
    void buscarMovies_ConDatos(){
        //Guardamos varias peliculas
        Movie movie1 = Movie.builder().title("Movie 1").director("Director 1").genre("Romance").releaseYear(2026).durationMinutes(120).build();
        Movie movie2 = Movie.builder().title("Movie 2").director("Director 2").genre("Comedia").releaseYear(2025).durationMinutes(120).build();

        List<Movie> movies = List.of(movie1, movie2);
        movieRepository.saveAll(movies);

        //Traemos todas las peliculas de BD
        List<Movie> moviesDB = movieRepository.findAll();
        for(Movie movie : moviesDB){
            System.out.println(movie);
        }

        //Comprobar que si hay peliculas
        assertNotNull(moviesDB);
        assertFalse(moviesDB.isEmpty());
        assertEquals(2, moviesDB.size());

        assertEquals("Movie 1", moviesDB.get(0).getTitle());
        assertEquals("Movie 2", moviesDB.get(1).getTitle());
    }

    @Test
    void borrarPeliculaPorId(){
        Movie movie = new Movie();
        movie.setTitle("The Dark Knight");
        movie.setDirector("Christopher Nolan");
        movie.setDurationMinutes(152);
        movie.setGenre("Action");
        movie = movieRepository.save(movie);

        movieRepository.delete(movie);

        assertEquals(0, movieRepository.count());
    }

    @Test
    void borrarPeliculas(){
        Movie movie1 = Movie.builder().title("Movie 1").director("Director 1").genre("Romance").releaseYear(2026).durationMinutes(120).build();
        Movie movie2 = Movie.builder().title("Movie 2").director("Director 2").genre("Comedia").releaseYear(2025).durationMinutes(110).build();

        movieRepository.saveAll(List.of(movie1, movie2));

        assertEquals(2, movieRepository.count());

        movieRepository.deleteAll();

        assertEquals(0, movieRepository.count());
    }

    @Test
    void buscarPeliculaPorId_Vacio(){
        Optional<Movie> movie = movieRepository.findById(1L);

        assertFalse(movie.isPresent());
    }

    @Test
    void buscarPeliculaPorId_Existente(){
        Movie movie = new Movie();
        movie.setTitle("Inception");
        movie.setDirector("Christopher Nolan");
        movie.setDurationMinutes(148);
        movie.setGenre("Sci-Fi");
        movie = movieRepository.save(movie);
        Long id = movie.getId();

        Optional<Movie> optional = movieRepository.findById(id);

        assertTrue(optional.isPresent());
        Movie movieFromDB = optional.get();
        assertEquals("Inception", movieFromDB.getTitle());
        assertEquals("Christopher Nolan", movieFromDB.getDirector());
    }

    @Test
    void existByIdTest(){
        assertFalse(movieRepository.existsById(1L));

        Movie movie = new Movie();
        movie.setTitle("Interstellar");
        movie.setDirector("Christopher Nolan");
        movie.setDurationMinutes(169);
        movie.setGenre("Sci-Fi");
        movie = movieRepository.save(movie);

        assertTrue(movieRepository.existsById(movie.getId()));
    }

}

package com.demo.repository;

import com.demo.model.Movie;
import com.demo.model.Review;
import com.demo.model.User;
import com.demo.model.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@DataJpaTest
public class ReviewRepositoryTest {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private UserRepository userRepository;

    Movie movie;
    Movie movie2;
    Movie movie3;


    @BeforeEach
    void setUp(){
        //guardar usuario
        User user1 = userRepository.save(User.builder().username("user").firstName("efew").email("ferfrfe@gmail.com").role(Role.ROLE_ADMIN).password("1234567").active(true).build());
        User user2 = userRepository.save(User.builder().username("user2").firstName("efew").email("ferfrreffe@gmail.com").role(Role.ROLE_USER).password("1234567").active(true).build());
        User user3 = userRepository.save(User.builder().username("user3").firstName("efew").email("ferffrefrfe@gmail.com").role(Role.ROLE_USER).password("1234567").active(true).build());
        userRepository.save(User.builder().username("user4").firstName("effrefew").email("ferffffrefrfe@gmail.com").role(Role.ROLE_USER).password("1234567").active(true).build());

        //guardar pelicula
        movie = movieRepository.save(Movie.builder().title("The Bourne Identity").genre("Action")
                .durationMinutes(119).releaseYear(2002).director("Doug Liman").build());
        movie2 = movieRepository.save(Movie.builder().title("The Age of Adaline").genre("Romance")
                .durationMinutes(112).releaseYear(2015).director("Lee Toland Krieger").build());
        movie3 = movieRepository.save(Movie.builder().title("The Dark Knight").genre("Action")
                .durationMinutes(152).releaseYear(2008).director("Christopher Nolan").build());

        reviewRepository.save(Review.builder().rating(5).title("guay").description("Me gusta la pelicula").user(user1).movie(movie).build());
        reviewRepository.save(Review.builder().rating(4).title("genial").description("Me gusta mucho la pelicula").user(user2).movie(movie2).build());
        reviewRepository.save(Review.builder().rating(3).title("fatal").description("No me gusta la pelicula").user(user3).movie(movie3).build());
        reviewRepository.save(Review.builder().rating(2).title("fatal").description("nada me gusta la pelicula").user(user3).movie(movie2).build());
    }

    @Test
    void findByMovieId_shouldReturnReviewsForSpecificMovie(){
        Long movieId = movie.getId();

        Review review = reviewRepository.findByMovieIdOrderByCreationDateDesc(movieId).getFirst();
        assertEquals(review.getMovie().getId(), movieId);
    }

    @Test
    void countByUser_Id(){
        Long userId = userRepository.findAll().get(2).getId();
        Long userId2 = userRepository.findAll().getFirst().getId();
        long count1 = reviewRepository.countByUser_Id(userId);
        assertEquals(2, count1);
        long count2 = reviewRepository.countByUser_Id(userId2);
        assertEquals(1, count2);
    }

    @Test
    void countByUser_Id_shouldReturnZeroIfUserHasNoReviews() {
        Long userId = userRepository.findAll().get(3).getId();
        long count = reviewRepository.countByUser_Id(userId);
        assertEquals(0, count);
    }

    @Test
    void findByUser_Id(){
        Long userId = userRepository.findAll().get(2).getId();
        List<Review> reviews = reviewRepository.findByUser_IdOrderByCreationDateDesc(userId);
        assertEquals(2, reviews.size());
    }

    @Test
    void findByRatingGreaterThanEqual(){
        List<Review> reviews = reviewRepository.findByRatingGreaterThanEqual(4);
        assertEquals(2, reviews.size());
        assertEquals(5, reviews.get(0).getRating());
        assertEquals(4, reviews.get(1).getRating());
        assertNotEquals(3, reviews.get(0).getRating());
    }
}

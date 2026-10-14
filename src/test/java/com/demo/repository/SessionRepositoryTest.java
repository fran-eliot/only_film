package com.demo.repository;

import com.demo.model.Movie;
import com.demo.model.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
@DataJpaTest

public class SessionRepositoryTest {

    @Autowired
    MovieRepository movieRepository;
    @Autowired
    SessionRepository sessionRepository;
    Movie movie1;
    @BeforeEach
    void setUp(){

        //movie
        movie1 = Movie.builder().title("peli1").build();
        movieRepository.save(movie1);

        //session
        Session session1 = Session.builder().price(12.50).language("VO").adMinutes(5).movie(movie1).build();
        Session session2 = Session.builder().price(15.00).language("doblada").adMinutes(10).movie(movie1).build();
        sessionRepository.save(session1);
        sessionRepository.save(session2);
    }

    @Test
    void findByMovieId(){
    List<Session> sesionesPeli1 = sessionRepository.findByMovie_Id(movie1.getId());
    assertEquals(2, sesionesPeli1.size());

    }

    @Test
    void findByMovieIdAndRoomId(){
    List<Session> sesionesPeli1Sala1 = sessionRepository.findByMovieIdAndRoomId(1L, 1L);
    assertEquals(0, sesionesPeli1Sala1.size());
    }

    @Test
    void findByLanguage(){
    List<Session> sesionesVO = sessionRepository.findByLanguage("VO");
    assertEquals(1, sesionesVO.size());
    }

    @Test
    void findByAdMinutesLessThanEqual() {
        List<Session> sesionesAd5 = sessionRepository.findByAdMinutesLessThanEqual(5);
        assertEquals(1, sesionesAd5.size());
    }

    @Test
    void findByAdMinutesLessThanEqualNegative() {
        List<Session> sesionesAdNeg = sessionRepository.findByAdMinutesLessThanEqual(-5);
        assertEquals(0, sesionesAdNeg.size());
    }
}

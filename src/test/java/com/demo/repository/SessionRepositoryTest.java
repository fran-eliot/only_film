package com.demo.repository;

import com.demo.model.Movie;
import com.demo.model.Room;
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
    @Autowired
    RoomRepository roomRepository;
    Movie movie1;
    Room room1;
    Room room2;
    Session session1;
    Session session2;
    @BeforeEach
    void setUp(){

        //movie
        movie1 = Movie.builder().title("peli1").build();
        movieRepository.save(movie1);

        // Dos salas distintas para comprobar el filtrado combinado.
        room1 = roomRepository.save(Room.builder().name("Sala A").capacity(50).active(true).build());
        room2 = roomRepository.save(Room.builder().name("Sala B").capacity(70).active(true).build());

        //session
        session1 = Session.builder().price(12.50).language("VO").adMinutes(5).movie(movie1).room(room1).build();
        session2 = Session.builder().price(15.00).language("doblada").adMinutes(10).movie(movie1).room(room2).build();
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
        List<Session> sesionesSala1 = sessionRepository.findByMovieIdAndRoomId(movie1.getId(), room1.getId());
        assertEquals(1, sesionesSala1.size());
        assertEquals(session1.getId(), sesionesSala1.get(0).getId());

        List<Session> sesionesSala2 = sessionRepository.findByMovieIdAndRoomId(movie1.getId(), room2.getId());
        assertEquals(1, sesionesSala2.size());
        assertEquals(session2.getId(), sesionesSala2.get(0).getId());

        Movie otraPelicula = movieRepository.save(Movie.builder().title("peli2").build());
        assertTrue(sessionRepository.findByMovieIdAndRoomId(otraPelicula.getId(), room1.getId()).isEmpty());
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

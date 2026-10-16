package com.demo.repository;

import com.demo.model.Movie;
import com.demo.model.Room;
import com.demo.model.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDateTime;
import java.time.ZoneId;
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
    void defaultStartTimeUsesMadridZoneForBuilderAndConstructor() {
        ZoneId madrid = ZoneId.of("Europe/Madrid");
        LocalDateTime before = LocalDateTime.now(madrid);

        Session built = Session.builder().build();
        Session constructed = new Session();

        LocalDateTime after = LocalDateTime.now(madrid);
        assertFalse(built.getStartTime().isBefore(before));
        assertFalse(built.getStartTime().isAfter(after));
        assertFalse(constructed.getStartTime().isBefore(before));
        assertFalse(constructed.getStartTime().isAfter(after));
    }

    @Test
    void findByMovieId(){
    List<Session> sesionesPeli1 = sessionRepository.findByMovie_Id(movie1.getId());
    assertEquals(2, sesionesPeli1.size());

    }

    @Test
    void findActiveSessionsByMovieOrdersByStartTimeAscending() {
        LocalDateTime early = LocalDateTime.of(2026, 11, 5, 16, 0);
        LocalDateTime late = early.plusHours(4);

        session1.setStartTime(late);
        session2.setStartTime(early);
        sessionRepository.saveAll(List.of(session1, session2));

        Session inactive = sessionRepository.save(Session.builder()
                .movie(movie1).room(room1).active(false)
                .startTime(early.minusHours(2)).build());

        Movie otherMovie = movieRepository.save(Movie.builder().title("peli2").build());
        Session otherMovieSession = sessionRepository.save(Session.builder()
                .movie(otherMovie).room(room1).active(true)
                .startTime(early.minusHours(1)).build());

        List<Session> movieSessions = sessionRepository
                .findByMovie_IdAndActiveTrueOrderByStartTimeAsc(movie1.getId());

        assertEquals(2, movieSessions.size());
        assertEquals(session2.getId(), movieSessions.get(0).getId());
        assertEquals(session1.getId(), movieSessions.get(1).getId());
        assertTrue(movieSessions.stream().allMatch(s -> Boolean.TRUE.equals(s.getActive())));
        assertTrue(movieSessions.stream().allMatch(s -> movie1.getId().equals(s.getMovie().getId())));
        assertFalse(movieSessions.stream().anyMatch(s -> inactive.getId().equals(s.getId())));

        List<Session> otherMovieSessions = sessionRepository
                .findByMovie_IdAndActiveTrueOrderByStartTimeAsc(otherMovie.getId());
        assertEquals(1, otherMovieSessions.size());
        assertEquals(otherMovieSession.getId(), otherMovieSessions.getFirst().getId());

        Movie withoutSessions = movieRepository.save(Movie.builder().title("sin sesiones").build());
        assertTrue(sessionRepository.findByMovie_IdAndActiveTrueOrderByStartTimeAsc(
                withoutSessions.getId()).isEmpty());
    }

    @Test
    void findActiveSessionsDescendingWithNullStartTimesLast() {
        LocalDateTime earlier = LocalDateTime.of(2026, 11, 5, 16, 0);
        LocalDateTime later = earlier.plusHours(4);

        session1.setStartTime(earlier);
        session2.setStartTime(later);
        sessionRepository.saveAll(List.of(session1, session2));

        Session withoutStartTime = sessionRepository.save(Session.builder()
                .movie(movie1).room(room1).active(true).startTime(null).build());
        Session inactive = sessionRepository.save(Session.builder()
                .movie(movie1).room(room2).active(false)
                .startTime(later.plusHours(2)).build());

        List<Session> activeSessions = sessionRepository
                .findAllByActiveTrueOrderByStartTimeDescNullsLast();

        assertEquals(3, activeSessions.size());
        assertEquals(session2.getId(), activeSessions.get(0).getId());
        assertEquals(session1.getId(), activeSessions.get(1).getId());
        assertEquals(withoutStartTime.getId(), activeSessions.get(2).getId());
        assertNull(activeSessions.get(2).getStartTime());
        assertTrue(activeSessions.stream().allMatch(s -> Boolean.TRUE.equals(s.getActive())));
        assertFalse(activeSessions.stream().anyMatch(s -> inactive.getId().equals(s.getId())));
    }

    @Test
    void existsByRoomIdIncludesInactiveSessionsAndExcludesUnusedRooms() {
        assertTrue(sessionRepository.existsByRoom_Id(room1.getId()));
        assertTrue(sessionRepository.existsByRoom_Id(room2.getId()));

        Room unusedRoom = roomRepository.save(Room.builder()
                .name("Sala sin sesiones").capacity(60).build());
        assertFalse(sessionRepository.existsByRoom_Id(unusedRoom.getId()));

        Room inactiveSessionRoom = roomRepository.save(Room.builder()
                .name("Sala con sesión inactiva").capacity(60).build());
        sessionRepository.save(Session.builder().movie(movie1)
                .room(inactiveSessionRoom).active(false).build());

        assertTrue(sessionRepository.existsByRoom_Id(inactiveSessionRoom.getId()));
        assertFalse(sessionRepository.existsByRoom_Id(Long.MAX_VALUE));
    }

    @Test
    void findAllMovieSessionsChronologicallyIncludingInactive() {
        LocalDateTime early = LocalDateTime.of(2026, 11, 5, 16, 0);
        LocalDateTime middle = early.plusHours(2);
        LocalDateTime late = early.plusHours(4);

        session1.setStartTime(late);
        session2.setStartTime(early);
        sessionRepository.saveAll(List.of(session1, session2));

        Session inactive = sessionRepository.save(Session.builder()
                .movie(movie1).room(room1).active(false)
                .startTime(middle).build());

        Movie otherMovie = movieRepository.save(Movie.builder().title("otra película").build());
        Session otherMovieSession = sessionRepository.save(Session.builder()
                .movie(otherMovie).room(room2).active(true)
                .startTime(early.plusHours(1)).build());

        List<Session> sessions = sessionRepository.findByMovie_IdOrderByStartTimeAsc(movie1.getId());

        assertEquals(3, sessions.size());
        assertEquals(session2.getId(), sessions.get(0).getId());
        assertEquals(inactive.getId(), sessions.get(1).getId());
        assertEquals(session1.getId(), sessions.get(2).getId());
        assertTrue(sessions.stream().allMatch(item -> movie1.getId().equals(item.getMovie().getId())));
        assertFalse(sessions.get(1).getActive());

        List<Session> otherSessions = sessionRepository.findByMovie_IdOrderByStartTimeAsc(otherMovie.getId());
        assertEquals(1, otherSessions.size());
        assertEquals(otherMovieSession.getId(), otherSessions.getFirst().getId());

        Movie withoutSessions = movieRepository.save(Movie.builder().title("sin proyecciones").build());
        assertTrue(sessionRepository.findByMovie_IdOrderByStartTimeAsc(withoutSessions.getId()).isEmpty());
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

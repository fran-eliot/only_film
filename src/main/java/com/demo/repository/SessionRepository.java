package com.demo.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.demo.model.Session;

import java.util.List;

public interface SessionRepository extends JpaRepository<Session, Long> {
    boolean existsByRoom_Id(Long roomId);
    List<Session> findByMovie_Id (Long id);
    List<Session> findByMovie_IdOrderByStartTimeAsc(Long movieId);
    List<Session> findByMovieIdAndRoomId (Long movieId, Long roomId);
    List<Session> findByLanguage(String language);
    List<Session> findByAdMinutesLessThanEqual(int i);
    List<Session> findAllByOrderByStartTimeDesc();

    @Query("SELECT s FROM Session s ORDER BY s.startTime DESC NULLS LAST")
    List<Session> findAllOrderByStartTimeDescNullsLast();

    @Query("SELECT s FROM Session s WHERE s.active = true ORDER BY s.startTime DESC NULLS LAST")
    List<Session> findAllByActiveTrueOrderByStartTimeDescNullsLast();

    List<Session> findByMovie_IdAndActiveTrueOrderByStartTimeAsc(Long movieId);
}
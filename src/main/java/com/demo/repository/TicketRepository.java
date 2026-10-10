package com.demo.repository;

import com.demo.model.Ticket;
import com.demo.model.enums.BuyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Optional;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select t from Ticket t where t.id = :id")
    Optional<Ticket> findByIdForCheckout(@org.springframework.data.repository.query.Param("id") Long id);

    List<Ticket> findBySession_Id(Long id);

    // Lock the existing seat inventory before changing a session's essential details.
    // Checkout locks individual tickets, so an edit waits for any in-flight purchase.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query(
            "select t from Ticket t where t.session.id = :sessionId order by t.id")
    List<Ticket> findBySessionIdForUpdate(
            @org.springframework.data.repository.query.Param("sessionId") Long sessionId);


    List<Ticket> findBySession_Movie_Id(Long id);

    List<Ticket> findByUser_IdOrderByBuyDateTimeDesc(Long id);

    List<Ticket> findByStatusOrderByBuyDateTimeDesc(BuyStatus status);

    List<Ticket> findByUser_IdAndStatusOrderByBuyDateTimeDesc(Long userId, BuyStatus status);

    boolean existsByUser_IdAndSession_Movie_IdAndStatus(Long userId, Long movieId, BuyStatus status);

    List<Ticket> findByUser_IdAndStatus(Long id, BuyStatus buyStatus);
}
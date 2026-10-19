package com.demo.repository;

import com.demo.model.Ticket;
import com.demo.model.Session;
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select s from Session s where s.id = :id")
    Optional<Session> lockSessionForInventoryGeneration(@org.springframework.data.repository.query.Param("id") Long id);

    boolean existsBySession_Id(Long id);

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

    /** Aggregate paid demonstration sales in the purchase-date interval [from, until). */
    @org.springframework.data.jpa.repository.Query("""
            select new com.demo.dto.DashboardSalesSummary(
                count(t),
                coalesce(sum(t.price), 0.0),
                coalesce(sum(t.snackPrice), 0.0))
            from Ticket t
            where t.status = com.demo.model.enums.BuyStatus.PAGADO
              and t.buyDateTime >= :from and t.buyDateTime < :until
              and (:movieId is null or t.session.movie.id = :movieId)
              and (:roomId is null or t.session.room.id = :roomId)
            """)
    com.demo.dto.DashboardSalesSummary aggregatePaidSales(
            @org.springframework.data.repository.query.Param("from") java.time.LocalDateTime from,
            @org.springframework.data.repository.query.Param("until") java.time.LocalDateTime until,
            @org.springframework.data.repository.query.Param("movieId") Long movieId,
            @org.springframework.data.repository.query.Param("roomId") Long roomId);

}

package com.demo.service;

import com.demo.model.Ticket;
import com.demo.model.Session;
import com.demo.model.Room;
import com.demo.model.User;
import com.demo.model.enums.BuyStatus;
import com.demo.repository.TicketRepository;
import com.demo.repository.RoomRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final RoomRepository roomRepository;

    private static final double PRICE_PALOMITAS_MEDIANAS = 4.50;
    private static final double PRICE_PALOMITAS_GRANDES  = 6.00;
    private static final double PRICE_REFRESCO           = 3.00;
    private static final double PRICE_PACK_DUO           = 8.50;
    private static final double PRICE_COMBO_FAMILIAR     = 14.00;

    public List <Ticket> getBySessionId(Long id){
        return ticketRepository.findBySession_Id(id);
    }
    public List <Ticket> getByMovieId(Long id){
        return ticketRepository.findBySession_Movie_Id(id);
    }
    public List<Ticket> getByUserId(Long id){
        return ticketRepository.findByUser_IdOrderByBuyDateTimeDesc(id);
    }
    public List<Ticket> findAll(){
        return ticketRepository.findAll();
    }
    public List<Ticket> findAllPagado(){
        return ticketRepository.findByStatusOrderByBuyDateTimeDesc(BuyStatus.PAGADO);
    }
    public List<Ticket> findPagadoByUserId(Long userId){
        return ticketRepository.findByUser_IdAndStatusOrderByBuyDateTimeDesc(userId, BuyStatus.PAGADO);
    }

    public Optional<Ticket> findById(Long id){
        return ticketRepository.findById(id);
    }

    public Ticket save(Ticket ticket){
        return ticketRepository.save(ticket);
    }

    public List <Ticket> generarTickets(Session session) {

        // Validar que session tenga una sala asociada
        if (session.getRoom() == null || session.getRoom().getId() == null) {
            return List.of();
        }

        // Recuperar el id de la room asociada a la sesion

        Room room = roomRepository.findById(session.getRoom().getId()).orElseThrow();

        Integer capacity = room.getCapacity();

        if (capacity == null || capacity <=0){
            return List.of();
        }

        //int seatsPerRow = 15;
        int seatsPerRow = (int) Math.ceil(Math.sqrt(capacity));

        List<Ticket> tickets = new ArrayList<>(capacity);
        char rowLetter = 'A';
        int created = 0;

        while (created < capacity && rowLetter <= 'Z') {

            for (int seatNum = 1;
                 seatNum <= seatsPerRow && created < capacity;
                 seatNum++, created++) {

                Ticket ticket = Ticket.builder()
                        .session(session)
                        .row(String.valueOf(rowLetter))
                        .seat(String.valueOf(seatNum))
                        .price(session.getPrice())
                        .discount(0.0)
                        .status(BuyStatus.LIBRE)
                        .build();

                tickets.add(ticket);
            }

            rowLetter++;
        }

        // Guardar datos
        if (!tickets.isEmpty()) {
            ticketRepository.saveAll(tickets);
        }


        return tickets;
    }

    @Transactional
    public void processCheckout(
            Long ticketId,
            User user,
            int qtyPalomitasMedianas,
            int qtyPalomitasGrandes,
            int qtyRefresco,
            int qtyPackDuo,
            int qtyComboFamiliar
    ) {

        Ticket ticket = ticketRepository.findByIdForCheckout(ticketId)
                .orElseThrow();

        if (ticket.getStatus() != BuyStatus.LIBRE) {
            throw new IllegalStateException("La entrada ya no está disponible");
        }
        if (qtyPalomitasMedianas < 0 || qtyPalomitasGrandes < 0 || qtyRefresco < 0
                || qtyPackDuo < 0 || qtyComboFamiliar < 0) {
            throw new IllegalArgumentException("Las cantidades no pueden ser negativas");
        }

        double snackTotal = calculateSnackTotal(
                qtyPalomitasMedianas,
                qtyPalomitasGrandes,
                qtyRefresco,
                qtyPackDuo,
                qtyComboFamiliar
        );

        ticket.setSnackPrice(snackTotal > 0 ? snackTotal : null);

        ticket.setPrice(ticket.getSession().getPrice());

        ticket.setBuyDateTime(LocalDateTime.now(ZoneId.of("Europe/Madrid")));

        ticket.setStatus(BuyStatus.PAGADO);

        if (user != null) {
            ticket.setUser(user);
        }

        ticket.setQrCode(generateQrCode(ticket.getId()));

        ticketRepository.save(ticket);
    }

    private double calculateSnackTotal(
            int qtyPalomitasMedianas,
            int qtyPalomitasGrandes,
            int qtyRefresco,
            int qtyPackDuo,
            int qtyComboFamiliar
    ) {

        return qtyPalomitasMedianas * PRICE_PALOMITAS_MEDIANAS
                + qtyPalomitasGrandes * PRICE_PALOMITAS_GRANDES
                + qtyRefresco * PRICE_REFRESCO
                + qtyPackDuo * PRICE_PACK_DUO
                + qtyComboFamiliar * PRICE_COMBO_FAMILIAR;
    }

    private String generateQrCode(Long ticketId) {

        return "ONLYFILM-" + ticketId + "-"
                + java.util.UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase();
    }
}
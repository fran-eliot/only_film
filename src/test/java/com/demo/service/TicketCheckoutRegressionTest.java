package com.demo.service;

import com.demo.model.Session;
import com.demo.model.Ticket;
import com.demo.model.User;
import com.demo.model.enums.BuyStatus;
import com.demo.repository.RoomRepository;
import com.demo.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketCheckoutRegressionTest {
    @Mock TicketRepository ticketRepository;
    @Mock RoomRepository roomRepository;
    @InjectMocks TicketService ticketService;

    private Ticket ticket(long id, BuyStatus status) {
        return Ticket.builder().id(id)
                .session(Session.builder().id(10L).price(10.0)
                        .movie(com.demo.model.Movie.builder().id(21L).active(true).build())
                        .room(com.demo.model.Room.builder().id(31L).active(true).build()).build())
                .status(status).build();
    }

    @Test
    void paidTicketCannotBePurchasedAgain() {
        when(ticketRepository.findByIdForCheckout(1L))
                .thenReturn(Optional.of(ticket(1L, BuyStatus.PAGADO)));
        User buyer = User.builder().id(2L).build();
        assertThrows(IllegalStateException.class,
                () -> ticketService.processCheckout(1L, buyer, 0, 0, 0, 0, 0));
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void cancelledTicketCannotBePurchased() {
        when(ticketRepository.findByIdForCheckout(2L))
                .thenReturn(Optional.of(ticket(2L, BuyStatus.CANCELADO)));
        User buyer = User.builder().id(3L).build();
        assertThrows(IllegalStateException.class,
                () -> ticketService.processCheckout(2L, buyer, 0, 0, 0, 0, 0));
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void negativeSnackQuantityIsRejected() {
        when(ticketRepository.findByIdForCheckout(3L))
                .thenReturn(Optional.of(ticket(3L, BuyStatus.LIBRE)));
        User buyer = User.builder().id(4L).build();
        assertThrows(IllegalArgumentException.class,
                () -> ticketService.processCheckout(3L, buyer, -1, 0, 0, 0, 0));
        verify(ticketRepository, never()).save(any());
    }
    @Test
    void inactiveSessionCannotBePurchased() {
        Ticket freeTicket = ticket(7L, BuyStatus.LIBRE);
        freeTicket.getSession().setActive(false);
        when(ticketRepository.findByIdForCheckout(7L)).thenReturn(Optional.of(freeTicket));
        assertThrows(IllegalStateException.class,
                () -> ticketService.processCheckout(7L, User.builder().id(7L).build(), 0, 0, 0, 0, 0));
        assertEquals(BuyStatus.LIBRE, freeTicket.getStatus());
        org.junit.jupiter.api.Assertions.assertNull(freeTicket.getUser());
        org.junit.jupiter.api.Assertions.assertNull(freeTicket.getQrCode());
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void missingSessionCannotBePurchased() {
        Ticket freeTicket = Ticket.builder().id(8L).status(BuyStatus.LIBRE).build();
        when(ticketRepository.findByIdForCheckout(8L)).thenReturn(Optional.of(freeTicket));
        assertThrows(IllegalStateException.class,
                () -> ticketService.processCheckout(8L, User.builder().id(8L).build(), 0, 0, 0, 0, 0));
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void inactiveMovieCannotBePurchased() {
        Ticket freeTicket = ticket(9L, BuyStatus.LIBRE);
        freeTicket.getSession().setMovie(com.demo.model.Movie.builder().id(20L).active(false).build());
        when(ticketRepository.findByIdForCheckout(9L)).thenReturn(Optional.of(freeTicket));
        assertThrows(IllegalStateException.class,
                () -> ticketService.processCheckout(9L, User.builder().id(9L).build(), 0, 0, 0, 0, 0));
        assertEquals(BuyStatus.LIBRE, freeTicket.getStatus());
        org.junit.jupiter.api.Assertions.assertNull(freeTicket.getUser());
        org.junit.jupiter.api.Assertions.assertNull(freeTicket.getQrCode());
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void missingMovieCannotBePurchased() {
        Ticket freeTicket = ticket(16L, BuyStatus.LIBRE);
        freeTicket.getSession().setMovie(null);
        when(ticketRepository.findByIdForCheckout(16L)).thenReturn(Optional.of(freeTicket));

        assertThrows(IllegalStateException.class,
                () -> ticketService.processCheckout(16L, User.builder().id(16L).build(), 0, 0, 0, 0, 0));

        assertEquals(BuyStatus.LIBRE, freeTicket.getStatus());
        org.junit.jupiter.api.Assertions.assertNull(freeTicket.getQrCode());
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void missingRoomCannotBePurchased() {
        Ticket freeTicket = ticket(17L, BuyStatus.LIBRE);
        freeTicket.getSession().setRoom(null);
        when(ticketRepository.findByIdForCheckout(17L)).thenReturn(Optional.of(freeTicket));

        assertThrows(IllegalStateException.class,
                () -> ticketService.processCheckout(17L, User.builder().id(17L).build(), 0, 0, 0, 0, 0));

        assertEquals(BuyStatus.LIBRE, freeTicket.getStatus());
        org.junit.jupiter.api.Assertions.assertNull(freeTicket.getQrCode());
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void activeMovieRemainsPurchasable() {
        Ticket freeTicket = ticket(10L, BuyStatus.LIBRE);
        freeTicket.getSession().setMovie(com.demo.model.Movie.builder().id(21L).active(true).build());
        when(ticketRepository.findByIdForCheckout(10L)).thenReturn(Optional.of(freeTicket));
        ticketService.processCheckout(10L, User.builder().id(10L).build(), 0, 0, 0, 0, 0);
        assertEquals(BuyStatus.PAGADO, freeTicket.getStatus());
        verify(ticketRepository).save(freeTicket);
    }

    @Test
    void inactiveRoomCannotBePurchased() {
        Ticket freeTicket = ticket(11L, BuyStatus.LIBRE);
        freeTicket.getSession().setRoom(com.demo.model.Room.builder().id(30L).active(false).build());
        when(ticketRepository.findByIdForCheckout(11L)).thenReturn(Optional.of(freeTicket));
        assertThrows(IllegalStateException.class,
                () -> ticketService.processCheckout(11L, User.builder().id(11L).build(), 0, 0, 0, 0, 0));
        assertEquals(BuyStatus.LIBRE, freeTicket.getStatus());
        org.junit.jupiter.api.Assertions.assertNull(freeTicket.getUser());
        org.junit.jupiter.api.Assertions.assertNull(freeTicket.getQrCode());
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void activeRoomRemainsPurchasable() {
        Ticket freeTicket = ticket(12L, BuyStatus.LIBRE);
        freeTicket.getSession().setRoom(com.demo.model.Room.builder().id(31L).active(true).build());
        when(ticketRepository.findByIdForCheckout(12L)).thenReturn(Optional.of(freeTicket));
        ticketService.processCheckout(12L, User.builder().id(12L).build(), 0, 0, 0, 0, 0);
        assertEquals(BuyStatus.PAGADO, freeTicket.getStatus());
        verify(ticketRepository).save(freeTicket);
    }

    @Test
    void checkoutAppliesTicketDiscountWithoutChangingSnackTotal() {
        Ticket freeTicket = ticket(13L, BuyStatus.LIBRE);
        freeTicket.setDiscount(2.50);
        when(ticketRepository.findByIdForCheckout(13L)).thenReturn(Optional.of(freeTicket));

        ticketService.processCheckout(13L, User.builder().id(13L).build(), 1, 0, 0, 0, 0);

        assertEquals(7.50, freeTicket.getPrice());
        assertEquals(4.50, freeTicket.getSnackPrice());
        assertEquals(BuyStatus.PAGADO, freeTicket.getStatus());
        verify(ticketRepository).save(freeTicket);
    }

    @Test
    void checkoutRejectsInvalidDiscountWithoutSellingTicket() {
        Ticket freeTicket = ticket(14L, BuyStatus.LIBRE);
        freeTicket.setDiscount(11.0);
        freeTicket.setSnackPrice(3.25);
        freeTicket.setPrice(10.0);
        when(ticketRepository.findByIdForCheckout(14L)).thenReturn(Optional.of(freeTicket));

        assertThrows(IllegalStateException.class,
                () -> ticketService.processCheckout(14L, User.builder().id(14L).build(), 1, 0, 0, 0, 0));

        assertEquals(BuyStatus.LIBRE, freeTicket.getStatus());
        assertEquals(3.25, freeTicket.getSnackPrice());
        assertEquals(10.0, freeTicket.getPrice());
        org.junit.jupiter.api.Assertions.assertNull(freeTicket.getUser());
        org.junit.jupiter.api.Assertions.assertNull(freeTicket.getQrCode());
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void checkoutRejectsNonFiniteSessionPriceWithoutMutatingTicket() {
        Ticket freeTicket = ticket(15L, BuyStatus.LIBRE);
        freeTicket.getSession().setPrice(Double.NaN);
        freeTicket.setSnackPrice(2.0);
        freeTicket.setPrice(10.0);
        when(ticketRepository.findByIdForCheckout(15L)).thenReturn(Optional.of(freeTicket));

        assertThrows(IllegalStateException.class,
                () -> ticketService.processCheckout(15L, User.builder().id(15L).build(), 1, 0, 0, 0, 0));

        assertEquals(BuyStatus.LIBRE, freeTicket.getStatus());
        assertEquals(2.0, freeTicket.getSnackPrice());
        assertEquals(10.0, freeTicket.getPrice());
        org.junit.jupiter.api.Assertions.assertNull(freeTicket.getUser());
        org.junit.jupiter.api.Assertions.assertNull(freeTicket.getQrCode());
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void excessiveSnackQuantityIsRejectedBeforeSaving() {
        when(ticketRepository.findByIdForCheckout(4L))
                .thenReturn(Optional.of(ticket(4L, BuyStatus.LIBRE)));
        assertThrows(IllegalArgumentException.class,
                () -> ticketService.processCheckout(4L, User.builder().id(5L).build(), 0, 101, 0, 0, 0));
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void maxIntegerSnackQuantityIsRejectedWithoutMutation() {
        Ticket freeTicket = ticket(5L, BuyStatus.LIBRE);
        when(ticketRepository.findByIdForCheckout(5L))
                .thenReturn(Optional.of(freeTicket));
        assertThrows(IllegalArgumentException.class,
                () -> ticketService.processCheckout(5L, User.builder().id(6L).build(), 0, 0, Integer.MAX_VALUE, 0, 0));
        assertEquals(BuyStatus.LIBRE, freeTicket.getStatus());
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void maximumAllowedSnackQuantityCanBePurchased() {
        Ticket freeTicket = ticket(6L, BuyStatus.LIBRE);
        when(ticketRepository.findByIdForCheckout(6L))
                .thenReturn(Optional.of(freeTicket));
        ticketService.processCheckout(6L, User.builder().id(7L).build(), 100, 0, 0, 0, 0);
        assertEquals(BuyStatus.PAGADO, freeTicket.getStatus());
        assertEquals(450.0, freeTicket.getSnackPrice());
        verify(ticketRepository).save(freeTicket);
    }

}

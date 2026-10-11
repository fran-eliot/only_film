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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketCheckoutRegressionTest {
    @Mock TicketRepository ticketRepository;
    @Mock RoomRepository roomRepository;
    @InjectMocks TicketService ticketService;

    private Ticket ticket(long id, BuyStatus status) {
        return Ticket.builder().id(id)
                .session(Session.builder().id(10L).price(10.0).build())
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
}

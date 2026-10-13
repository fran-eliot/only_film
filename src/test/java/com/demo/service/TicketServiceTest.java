package com.demo.service;

import com.demo.model.Room;
import com.demo.model.Session;
import com.demo.model.Ticket;
import com.demo.model.User;
import com.demo.model.enums.BuyStatus;
import com.demo.repository.RoomRepository;
import com.demo.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private TicketService ticketService;

    private Room room;
    private Session session;

    @BeforeEach
    public void setUp() {
        room = Room.builder()
                .id(1L)
                .name("Sala 1")
                .capacity(4)
                .active(true)
                .build();

        session = Session.builder()
                .id(1L)
                .price(9.50)
                .room(room)
                .movie(com.demo.model.Movie.builder().id(1L).active(true).build())
                .build();
    }

    @Test
    @DisplayName("findAll: devuelve todos los tickets")
    public void findAll_returnsAllTickets() {
        // Arrange
        List<Ticket> tickets = List.of(
                Ticket.builder().id(1L).row("A").seat("1").build(),
                Ticket.builder().id(2L).row("A").seat("2").build()
        );

        when(ticketRepository.findAll()).thenReturn(tickets);

        // Act
        List<Ticket> result = ticketService.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("A", result.get(0).getRow());
        assertEquals("1", result.get(0).getSeat());

        verify(ticketRepository, times(1)).findAll();
        verifyNoMoreInteractions(ticketRepository, roomRepository);
    }

    @Test
    @DisplayName("findById: ticket existente -> devuelve Optional con ticket")
    public void findById_existingTicket_returnsOptionalWithTicket() {
        // Arrange
        Ticket ticket = Ticket.builder()
                .id(1L)
                .row("B")
                .seat("5")
                .status(BuyStatus.LIBRE)
                .build();

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        // Act
        Optional<Ticket> result = ticketService.findById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        assertEquals("B", result.get().getRow());
        assertEquals("5", result.get().getSeat());
        assertEquals(BuyStatus.LIBRE, result.get().getStatus());

        verify(ticketRepository, times(1)).findById(1L);
        verifyNoMoreInteractions(ticketRepository, roomRepository);
    }

    @Test
    void updateAdminDiscountRejectsNullFormWithoutRepositoryAccess() {
        assertThrows(IllegalArgumentException.class, () -> ticketService.updateAdminDiscount(null));
        verifyNoInteractions(ticketRepository, roomRepository);
    }

    @Test
    void updateAdminDiscountRejectsMissingIdWithoutRepositoryAccess() {
        com.demo.dto.TicketAdminForm form = new com.demo.dto.TicketAdminForm();
        form.setDiscount(1.0);

        assertThrows(IllegalArgumentException.class, () -> ticketService.updateAdminDiscount(form));

        verifyNoInteractions(ticketRepository, roomRepository);
    }

    @Test
    void updateAdminDiscountRejectsNaNWithoutSaving() {
        Ticket ticket = Ticket.builder().id(1L).session(session)
                .status(BuyStatus.LIBRE).discount(0.0).build();
        com.demo.dto.TicketAdminForm form = new com.demo.dto.TicketAdminForm();
        form.setId(1L);
        form.setDiscount(Double.NaN);
        when(ticketRepository.findByIdForCheckout(1L)).thenReturn(Optional.of(ticket));

        assertThrows(IllegalArgumentException.class, () -> ticketService.updateAdminDiscount(form));

        assertEquals(0.0, ticket.getDiscount());
        verify(ticketRepository).findByIdForCheckout(1L);
        verify(ticketRepository, never()).save(any());
        verifyNoMoreInteractions(ticketRepository, roomRepository);
    }

    @Test
    void updateAdminDiscountRejectsInfinityWithoutSaving() {
        Ticket ticket = Ticket.builder().id(1L).session(session)
                .status(BuyStatus.LIBRE).discount(0.0).build();
        com.demo.dto.TicketAdminForm form = new com.demo.dto.TicketAdminForm();
        form.setId(1L);
        form.setDiscount(Double.POSITIVE_INFINITY);
        when(ticketRepository.findByIdForCheckout(1L)).thenReturn(Optional.of(ticket));

        assertThrows(IllegalArgumentException.class, () -> ticketService.updateAdminDiscount(form));

        assertEquals(0.0, ticket.getDiscount());
        verify(ticketRepository).findByIdForCheckout(1L);
        verify(ticketRepository, never()).save(any());
        verifyNoMoreInteractions(ticketRepository, roomRepository);
    }

    @Test
    @DisplayName("admin: descuento permitido solo en entrada libre")
    public void updateAdminDiscount_changesOnlyFreeTicketDiscount() {
        Ticket ticket = Ticket.builder().id(1L).row("C").seat("3")
                .price(9.50).session(session).discount(0.0).status(BuyStatus.LIBRE).build();
        com.demo.dto.TicketAdminForm form = new com.demo.dto.TicketAdminForm();
        form.setId(1L);
        form.setDiscount(2.0);
        when(ticketRepository.findByIdForCheckout(1L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        Ticket updated = ticketService.updateAdminDiscount(form);

        assertEquals(2.0, updated.getDiscount());
        assertEquals("C", updated.getRow());
        assertEquals(9.50, updated.getPrice());
        assertEquals(BuyStatus.LIBRE, updated.getStatus());
        verify(ticketRepository).save(ticket);
    }

    @Test
    void adminDiscountRejectsAmountAboveCurrentSessionPrice() {
        Session discountedSession = Session.builder().price(5.0).build();
        Ticket ticket = Ticket.builder().id(2L).session(discountedSession)
                .price(10.0).status(BuyStatus.LIBRE).build();
        com.demo.dto.TicketAdminForm form = new com.demo.dto.TicketAdminForm();
        form.setId(2L);
        form.setDiscount(7.0);
        when(ticketRepository.findByIdForCheckout(2L)).thenReturn(Optional.of(ticket));

        assertThrows(IllegalArgumentException.class, () -> ticketService.updateAdminDiscount(form));
        assertNull(ticket.getDiscount());
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void adminDiscountAllowsAmountBelowCurrentSessionPrice() {
        Session repricedSession = Session.builder().price(12.0).build();
        Ticket ticket = Ticket.builder().id(3L).session(repricedSession)
                .price(7.0).status(BuyStatus.LIBRE).build();
        com.demo.dto.TicketAdminForm form = new com.demo.dto.TicketAdminForm();
        form.setId(3L);
        form.setDiscount(10.0);
        when(ticketRepository.findByIdForCheckout(3L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        Ticket updated = ticketService.updateAdminDiscount(form);

        assertEquals(10.0, updated.getDiscount());
        verify(ticketRepository).save(ticket);
    }

    @Test
    @DisplayName("admin: entrada pagada no admite cambios")
    public void updateAdminDiscount_rejectsPaidTicket() {
        Ticket ticket = Ticket.builder().id(1L).price(9.5).status(BuyStatus.PAGADO).build();
        com.demo.dto.TicketAdminForm form = new com.demo.dto.TicketAdminForm();
        form.setId(1L);
        form.setDiscount(1.0);
        when(ticketRepository.findByIdForCheckout(1L)).thenReturn(Optional.of(ticket));
        assertThrows(IllegalStateException.class, () -> ticketService.updateAdminDiscount(form));
        verify(ticketRepository, never()).save(any());
    }

    @Test
    @DisplayName("getBySessionId: devuelve tickets de una sesión")
    public void getBySessionId_returnsTicketsFromSession() {
        // Arrange
        List<Ticket> tickets = List.of(
                Ticket.builder().id(1L).session(session).build(),
                Ticket.builder().id(2L).session(session).build()
        );

        when(ticketRepository.findBySession_Id(1L)).thenReturn(tickets);

        // Act
        List<Ticket> result = ticketService.getBySessionId(1L);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());

        verify(ticketRepository, times(1)).findBySession_Id(1L);
        verifyNoMoreInteractions(ticketRepository, roomRepository);
    }

    @Test
    @DisplayName("getByMovieId: devuelve tickets asociados a una película")
    public void getByMovieId_returnsTicketsFromMovie() {
        // Arrange
        List<Ticket> tickets = List.of(
                Ticket.builder().id(1L).build(),
                Ticket.builder().id(2L).build()
        );

        when(ticketRepository.findBySession_Movie_Id(10L)).thenReturn(tickets);

        // Act
        List<Ticket> result = ticketService.getByMovieId(10L);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());

        verify(ticketRepository, times(1)).findBySession_Movie_Id(10L);
        verifyNoMoreInteractions(ticketRepository, roomRepository);
    }

    @Test
    @DisplayName("getByUserId: devuelve tickets de un usuario")
    public void getByUserId_returnsTicketsFromUser() {
        // Arrange
        List<Ticket> tickets = List.of(
                Ticket.builder().id(1L).build()
        );

        when(ticketRepository.findByUser_IdOrderByBuyDateTimeDesc(7L)).thenReturn(tickets);

        // Act
        List<Ticket> result = ticketService.getByUserId(7L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());

        verify(ticketRepository, times(1)).findByUser_IdOrderByBuyDateTimeDesc(7L);
        verifyNoMoreInteractions(ticketRepository, roomRepository);
    }

    @Test
    @DisplayName("generarTickets: sesión con sala válida -> genera tickets según capacidad")
    public void generarTickets_withValidSessionAndRoom_generatesTicketsByRoomCapacity() {
        // Arrange
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(ticketRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        List<Ticket> result = ticketService.generarTickets(session);

        // Assert
        assertNotNull(result);
        assertEquals(4, result.size());

        assertEquals("A", result.get(0).getRow());
        assertEquals("1", result.get(0).getSeat());
        assertEquals(9.50, result.get(0).getPrice());
        assertEquals(0.0, result.get(0).getDiscount());
        assertEquals(BuyStatus.LIBRE, result.get(0).getStatus());
        assertSame(session, result.get(0).getSession());

        ArgumentCaptor<List<Ticket>> captor = ArgumentCaptor.forClass(List.class);
        verify(ticketRepository, times(1)).saveAll(captor.capture());

        List<Ticket> savedTickets = captor.getValue();
        assertEquals(4, savedTickets.size());

        verify(roomRepository, times(1)).findById(1L);
        verify(ticketRepository).existsBySession_Id(1L);
        verifyNoMoreInteractions(ticketRepository, roomRepository);
    }

    @Test
    void generarTicketsRejectsNegativeSessionPriceWithoutPersisting() {
        session.setPrice(-0.01);

        assertThrows(IllegalArgumentException.class, () -> ticketService.generarTickets(session));

        verifyNoInteractions(ticketRepository, roomRepository);
    }

    @Test
    void generarTicketsRejectsNonFiniteSessionPriceWithoutPersisting() {
        session.setPrice(Double.NaN);

        assertThrows(IllegalArgumentException.class, () -> ticketService.generarTickets(session));

        verifyNoInteractions(ticketRepository, roomRepository);
    }

    @Test
    void generarTicketsRejectsNullSessionPriceWithoutPersisting() {
        session.setPrice(null);

        assertThrows(IllegalArgumentException.class, () -> ticketService.generarTickets(session));

        verifyNoInteractions(ticketRepository, roomRepository);
    }

    @Test
    void generarTicketsRejectsInactiveSessionWithoutPersisting() {
        session.setActive(false);

        assertThrows(IllegalStateException.class, () -> ticketService.generarTickets(session));

        verifyNoInteractions(ticketRepository, roomRepository);
    }

    @Test
    void generarTicketsRejectsMissingMovieWithoutPersisting() {
        session.setMovie(null);

        assertThrows(IllegalStateException.class, () -> ticketService.generarTickets(session));

        verifyNoInteractions(ticketRepository, roomRepository);
    }

    @Test
    void generarTicketsRejectsInactiveMovieWithoutPersisting() {
        session.getMovie().setActive(false);

        assertThrows(IllegalStateException.class, () -> ticketService.generarTickets(session));

        verifyNoInteractions(ticketRepository, roomRepository);
    }

    @Test
    void generarTicketsRejectsUnsavedMovieWithoutPersisting() {
        session.getMovie().setId(null);

        assertThrows(IllegalArgumentException.class, () -> ticketService.generarTickets(session));

        verifyNoInteractions(ticketRepository, roomRepository);
    }

    @Test
    void generarTicketsRejectsUnsavedSessionWithoutPersisting() {
        session.setId(null);

        assertThrows(IllegalArgumentException.class, () -> ticketService.generarTickets(session));

        verifyNoInteractions(ticketRepository, roomRepository);
    }

    @Test
    void generarTicketsRejectsInactiveRoomWithoutPersisting() {
        room.setActive(false);
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));

        assertThrows(IllegalStateException.class, () -> ticketService.generarTickets(session));

        verify(roomRepository).findById(1L);
        verify(ticketRepository, never()).saveAll(anyList());
        verifyNoMoreInteractions(ticketRepository, roomRepository);
    }

    @Test
    void generarTicketsRejectsInactiveRoomWithZeroCapacity() {
        room.setActive(false);
        room.setCapacity(0);
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));

        assertThrows(IllegalStateException.class, () -> ticketService.generarTickets(session));

        verify(roomRepository).findById(1L);
        verifyNoInteractions(ticketRepository);
    }

    @Test
    void generarTicketsRejectsInactiveRoomWithNullCapacity() {
        room.setActive(false);
        room.setCapacity(null);
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));

        assertThrows(IllegalStateException.class, () -> ticketService.generarTickets(session));

        verify(roomRepository).findById(1L);
        verifyNoInteractions(ticketRepository);
    }

    @Test
    void generarTicketsHasTransactionalBoundary() throws NoSuchMethodException {
        org.springframework.transaction.annotation.Transactional annotation =
                TicketService.class.getMethod("generarTickets", Session.class)
                        .getAnnotation(org.springframework.transaction.annotation.Transactional.class);

        assertNotNull(annotation);
        assertFalse(annotation.readOnly());
    }

    @Test
    void generarTicketsBeyondRowZPreservesFullCapacity() {
        room.setCapacity(800);
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(ticketRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<Ticket> result = ticketService.generarTickets(session);

        assertEquals(800, result.size());
        assertEquals("A", result.get(0).getRow());
        assertEquals("Z", result.get(25 * 29).getRow());
        assertEquals("AA", result.get(26 * 29).getRow());
        assertEquals("AB", result.get(27 * 29).getRow());
        assertEquals("17", result.get(799).getSeat());
        verify(ticketRepository).saveAll(anyList());
    }

    @Test
    void generarTicketsDoesNotDuplicateExistingSessionInventory() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(ticketRepository.existsBySession_Id(1L)).thenReturn(true);

        List<Ticket> result = ticketService.generarTickets(session);

        assertTrue(result.isEmpty());
        verify(ticketRepository).existsBySession_Id(1L);
        verify(ticketRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("generarTickets: sesión sin sala -> devuelve lista vacía")
    public void generarTickets_withoutRoom_returnsEmptyList() {
        // Arrange
        Session sessionWithoutRoom = Session.builder()
                .id(1L)
                .price(9.50)
                .movie(com.demo.model.Movie.builder().id(1L).active(true).build())
                .room(null)
                .build();

        // Act
        List<Ticket> result = ticketService.generarTickets(sessionWithoutRoom);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verifyNoInteractions(ticketRepository, roomRepository);
    }

    @Test
    @DisplayName("generarTickets: sala sin id -> devuelve lista vacía")
    public void generarTickets_withRoomWithoutId_returnsEmptyList() {
        // Arrange
        Room roomWithoutId = Room.builder()
                .name("Sala sin id")
                .capacity(4)
                .build();

        Session sessionWithRoomWithoutId = Session.builder()
                .id(1L)
                .price(9.50)
                .movie(com.demo.model.Movie.builder().id(1L).active(true).build())
                .room(roomWithoutId)
                .build();

        // Act
        List<Ticket> result = ticketService.generarTickets(sessionWithRoomWithoutId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verifyNoInteractions(ticketRepository, roomRepository);
    }

    @Test
    @DisplayName("generarTickets: sala con capacidad nula -> devuelve lista vacía")
    public void generarTickets_withNullCapacity_returnsEmptyList() {
        // Arrange
        Room roomWithoutCapacity = Room.builder()
                .id(1L)
                .name("Sala sin capacidad")
                .active(true)
                .capacity(null)
                .build();

        when(roomRepository.findById(1L)).thenReturn(Optional.of(roomWithoutCapacity));

        // Act
        List<Ticket> result = ticketService.generarTickets(session);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(roomRepository, times(1)).findById(1L);
        verify(ticketRepository, never()).saveAll(anyList());
        verifyNoMoreInteractions(ticketRepository, roomRepository);
    }

    @Test
    @DisplayName("generarTickets: sala con capacidad cero -> devuelve lista vacía")
    public void generarTickets_withZeroCapacity_returnsEmptyList() {
        // Arrange
        Room roomWithZeroCapacity = Room.builder()
                .id(1L)
                .name("Sala vacía")
                .active(true)
                .capacity(0)
                .build();

        when(roomRepository.findById(1L)).thenReturn(Optional.of(roomWithZeroCapacity));

        // Act
        List<Ticket> result = ticketService.generarTickets(session);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(roomRepository, times(1)).findById(1L);
        verify(ticketRepository, never()).saveAll(anyList());
        verifyNoMoreInteractions(ticketRepository, roomRepository);
    }

    @Test
    @DisplayName("generarTickets: sala inexistente -> lanza NoSuchElementException")
    public void generarTickets_withNonExistingRoom_throwsNoSuchElementException() {
        // Arrange
        when(roomRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NoSuchElementException.class, () -> ticketService.generarTickets(session));

        verify(roomRepository, times(1)).findById(1L);
        verify(ticketRepository, never()).saveAll(anyList());
        verifyNoMoreInteractions(ticketRepository, roomRepository);
    }

    @Test
    @DisplayName("processCheckout: ticket existente con snacks y usuario -> marca como pagado y guarda")
    public void processCheckout_existingTicketWithSnacksAndUser_marksAsPaidAndSaves() {
        // Arrange
        User user = User.builder()
                .id(5L)
                .username("john")
                .email("john@example.com")
                .build();

        Ticket ticket = Ticket.builder()
                .id(1L)
                .session(session)
                .row("A")
                .seat("1")
                .status(BuyStatus.LIBRE)
                .build();

        when(ticketRepository.findByIdForCheckout(1L)).thenReturn(Optional.of(ticket));

        // Act
        ticketService.processCheckout(
                1L,
                user,
                1, // palomitas medianas = 4.50
                1, // palomitas grandes = 6.00
                1, // refresco = 3.00
                0,
                0
        );

        // Assert
        assertEquals(BuyStatus.PAGADO, ticket.getStatus());
        assertEquals(9.50, ticket.getPrice());
        assertEquals(13.50, ticket.getSnackPrice());
        assertNotNull(ticket.getBuyDateTime());
        assertSame(user, ticket.getUser());
        assertNotNull(ticket.getQrCode());
        assertTrue(ticket.getQrCode().startsWith("ONLYFILM-1-"));

        verify(ticketRepository, times(1)).findByIdForCheckout(1L);
        verify(ticketRepository, times(1)).save(ticket);
        verifyNoMoreInteractions(ticketRepository, roomRepository);
    }

    @Test
    @DisplayName("processCheckout: ticket existente sin snacks -> snackPrice queda null")
    public void processCheckout_existingTicketWithoutSnacks_setsSnackPriceNull() {
        // Arrange
        Ticket ticket = Ticket.builder()
                .id(1L)
                .session(session)
                .status(BuyStatus.LIBRE)
                .build();

        when(ticketRepository.findByIdForCheckout(1L)).thenReturn(Optional.of(ticket));

        // Act
        ticketService.processCheckout(
                1L,
                User.builder().id(6L).build(),
                0,
                0,
                0,
                0,
                0
        );

        // Assert
        assertEquals(BuyStatus.PAGADO, ticket.getStatus());
        assertEquals(9.50, ticket.getPrice());
        assertNull(ticket.getSnackPrice());
        assertEquals(6L, ticket.getUser().getId());
        assertNotNull(ticket.getBuyDateTime());
        assertNotNull(ticket.getQrCode());
        assertTrue(ticket.getQrCode().startsWith("ONLYFILM-1-"));

        verify(ticketRepository, times(1)).findByIdForCheckout(1L);
        verify(ticketRepository, times(1)).save(ticket);
        verifyNoMoreInteractions(ticketRepository, roomRepository);
    }

    @Test
    void processCheckoutRejectsNullTicketIdWithoutRepositoryAccess() {
        assertThrows(IllegalArgumentException.class, () ->
                ticketService.processCheckout(null, User.builder().id(6L).build(), 0, 0, 0, 0, 0));

        verifyNoInteractions(ticketRepository, roomRepository);
    }

    @Test
    @DisplayName("processCheckout: ticket inexistente -> lanza NoSuchElementException")
    public void processCheckout_nonExistingTicket_throwsNoSuchElementException() {
        // Arrange
        when(ticketRepository.findByIdForCheckout(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(NoSuchElementException.class, () ->
                ticketService.processCheckout(
                        99L,
                        User.builder().id(99L).build(),
                        0,
                        0,
                        0,
                        0,
                        0
                )
        );

        verify(ticketRepository, times(1)).findByIdForCheckout(99L);
        verify(ticketRepository, never()).save(any());
        verifyNoMoreInteractions(ticketRepository, roomRepository);
    }

    @Test
    @DisplayName("findAllPagado: devuelve tickets pagados ordenados por fecha descendente")
    void findAllPagado_returnsPaidTickets() {
        List<Ticket> tickets = List.of(
                Ticket.builder().id(1L).status(BuyStatus.PAGADO).build(),
                Ticket.builder().id(2L).status(BuyStatus.PAGADO).build()
        );

        when(ticketRepository.findByStatusOrderByBuyDateTimeDesc(BuyStatus.PAGADO))
                .thenReturn(tickets);

        List<Ticket> result = ticketService.findAllPagado();

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(ticket -> ticket.getStatus() == BuyStatus.PAGADO));

        verify(ticketRepository).findByStatusOrderByBuyDateTimeDesc(BuyStatus.PAGADO);
        verifyNoMoreInteractions(ticketRepository, roomRepository);
    }

    @Test
    @DisplayName("findPagadoByUserId: devuelve tickets pagados de un usuario")
    void findPagadoByUserId_returnsPaidTicketsFromUser() {
        List<Ticket> tickets = List.of(
                Ticket.builder().id(1L).status(BuyStatus.PAGADO).build()
        );

        when(ticketRepository.findByUser_IdAndStatusOrderByBuyDateTimeDesc(7L, BuyStatus.PAGADO))
                .thenReturn(tickets);

        List<Ticket> result = ticketService.findPagadoByUserId(7L);

        assertEquals(1, result.size());
        assertEquals(BuyStatus.PAGADO, result.getFirst().getStatus());

        verify(ticketRepository)
                .findByUser_IdAndStatusOrderByBuyDateTimeDesc(7L, BuyStatus.PAGADO);
        verifyNoMoreInteractions(ticketRepository, roomRepository);
    }

    @Test
    @DisplayName("processCheckout: calcula correctamente todos los tipos de snacks")
    void processCheckout_withAllSnackTypes_calculatesTotalSnackPrice() {
        Ticket ticket = Ticket.builder()
                .id(10L)
                .session(session)
                .status(BuyStatus.LIBRE)
                .build();

        when(ticketRepository.findByIdForCheckout(10L)).thenReturn(Optional.of(ticket));

        ticketService.processCheckout(
                10L,
                User.builder().id(11L).build(),
                2, // 2 * 4.50 = 9.00
                1, // 1 * 6.00 = 6.00
                3, // 3 * 3.00 = 9.00
                2, // 2 * 8.50 = 17.00
                1  // 1 * 14.00 = 14.00
        );

        assertEquals(BuyStatus.PAGADO, ticket.getStatus());
        assertEquals(9.50, ticket.getPrice());
        assertEquals(55.00, ticket.getSnackPrice());
        assertNotNull(ticket.getQrCode());
        assertTrue(ticket.getQrCode().startsWith("ONLYFILM-10-"));

        verify(ticketRepository).findByIdForCheckout(10L);
        verify(ticketRepository).save(ticket);
        verifyNoMoreInteractions(ticketRepository, roomRepository);
    }
}

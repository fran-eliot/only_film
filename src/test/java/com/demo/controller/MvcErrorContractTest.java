package com.demo.controller;

import com.demo.model.Ticket;
import com.demo.model.enums.BuyStatus;
import com.demo.repository.TicketRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** HTTP and view contracts for expected errors, including recoverable form validation. */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class MvcErrorContractTest {
    @Autowired MockMvc mvc;
    @Autowired TicketRepository tickets;

    @Test
    void nonexistentEditableResourceReturns404() throws Exception {
        mvc.perform(get("/sessions/edit/999999999"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/movies/edit/999999999"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/tickets/999999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidSessionCommandRendersValidationErrors() throws Exception {
        mvc.perform(post("/sessions").param("price", "-1.0"))
                .andExpect(status().isOk())
                .andExpect(view().name("sessions/session-form"))
                .andExpect(model().attributeHasFieldErrors("proyeccion", "price"))
                .andExpect(model().attributeExists("movies", "rooms"));
    }

    @Test
    void unavailableCheckoutReturns409() throws Exception {
        Ticket sold = tickets.save(Ticket.builder().status(BuyStatus.PAGADO).build());
        mvc.perform(get("/tickets/" + sold.getId() + "/checkout"))
                .andExpect(status().isConflict());
    }
}

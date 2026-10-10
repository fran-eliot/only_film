package com.demo.controller;

import com.demo.model.Ticket;
import com.demo.model.User;
import com.demo.model.enums.Role;
import com.demo.repository.SessionRepository;
import com.demo.repository.UserRepository;
import com.demo.service.TicketService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@AllArgsConstructor
public class TicketController {

    //private final TicketRepository ticketRepository;
    private final TicketService ticketService;

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;

    //  Precios de snacks
    private static final double PRICE_PALOMITAS_MEDIANAS = 4.50;
    private static final double PRICE_PALOMITAS_GRANDES  = 6.00;
    private static final double PRICE_REFRESCO           = 3.00;
    private static final double PRICE_PACK_DUO           = 8.50;
    private static final double PRICE_COMBO_FAMILIAR     = 14.00;

    //  Listado de tickets
    @GetMapping("tickets")
    public String getTickets(Model model, @AuthenticationPrincipal User user) {
        List<Ticket> tickets;
        boolean isAdmin = user.getRole() == Role.ROLE_ADMIN;
        if (isAdmin) {
            tickets = ticketService.findAllPagado();
            model.addAttribute("title", "Entradas vendidas");
            model.addAttribute("isAdmin", true);
        } else {
            tickets = ticketService.getByUserId(user.getId());
            model.addAttribute("title", "Mis entradas");
            model.addAttribute("isAdmin", false);
        }
        model.addAttribute("tickets", tickets);
        model.addAttribute("numTickets", tickets != null ? tickets.size() : 0);
        return "tickets/ticket-list";
    }

    //  Detalle de un ticket
    @GetMapping("tickets/{id}")
    public String ticketDetail(@PathVariable Long id, Model model, @AuthenticationPrincipal User user) {
        Ticket ticket = ticketService.findById(id).orElseThrow();
        if (user == null || (user.getRole() != Role.ROLE_ADMIN
                && (ticket.getUser() == null || !ticket.getUser().getId().equals(user.getId())))) {
            throw new AccessDeniedException("No puedes consultar esta entrada");
        }
        model.addAttribute("ticket", ticket);
        return "tickets/ticket-detail";
    }

    //  Edición de ticket (admin)
    @GetMapping("tickets/edit/{id}")
    public String editTicket(@PathVariable Long id, Model model) {
        model.addAttribute("ticket", ticketService.findById(id).orElseThrow());
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("sessions", sessionRepository.findAll());
        return "tickets/ticket-form";
    }

    //  GET /tickets/{id}/checkout  — Pantalla de checkout
    @GetMapping("tickets/{id}/checkout")
    public String checkoutGet(@PathVariable Long id, Model model) {
        Ticket ticket = ticketService.findById(id).orElseThrow();
        model.addAttribute("ticket", ticket);
        return "tickets/ticket-checkout";
    }

    //  POST /tickets/{id}/checkout, procesa snacks + pago + QR en el service
    @PostMapping("tickets/{id}/checkout")
    public String checkoutPost(
            @PathVariable Long id,
            @AuthenticationPrincipal User user,

            @RequestParam(defaultValue = "0") int qtyPalomitasMedianas,
            @RequestParam(defaultValue = "0") int qtyPalomitasGrandes,
            @RequestParam(defaultValue = "0") int qtyRefresco,
            @RequestParam(defaultValue = "0") int qtyPackDuo,
            @RequestParam(defaultValue = "0") int qtyComboFamiliar,

            RedirectAttributes redirectAttributes) {

        ticketService.processCheckout(
                id,
                user,
                qtyPalomitasMedianas,
                qtyPalomitasGrandes,
                qtyRefresco,
                qtyPackDuo,
                qtyComboFamiliar
        );

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Disfruta la película 🎬"
        );

        return "redirect:/tickets/" + id;
    }

    //  GET /tickets/buy/{id} redirige al checkout
    @GetMapping("tickets/buy/{id}")
    public String buyTicket(@PathVariable Long id) {
        return "redirect:/tickets/" + id + "/checkout";
    }


    // POST tickets/save
    @PostMapping("tickets/save")
    public String saveTicket(@ModelAttribute Ticket ticket) {

        ticketService.save(ticket);

        return "redirect:/tickets";
    }
}

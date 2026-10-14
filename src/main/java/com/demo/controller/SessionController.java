package com.demo.controller;

    import com.demo.model.Session;
    import com.demo.dto.SessionForm;
    import com.demo.repository.SessionRepository;
    import com.demo.repository.MovieRepository;
    import com.demo.repository.RoomRepository;
    import com.demo.repository.TicketRepository;
    import com.demo.service.SessionManagementService;
    import com.demo.service.TicketService;
    import lombok.AllArgsConstructor;
    import jakarta.validation.Valid;
    import org.springframework.validation.BindingResult;
    import org.springframework.stereotype.Controller;
    import org.springframework.ui.Model;
    import org.springframework.http.HttpStatus;
    import org.springframework.web.server.ResponseStatusException;
    import org.springframework.web.bind.annotation.GetMapping;
    import org.springframework.web.bind.annotation.ModelAttribute;
    import org.springframework.web.bind.annotation.PathVariable;
    import org.springframework.web.bind.annotation.PostMapping;
    import java.util.List;
    import java.time.LocalDateTime;
    import java.time.ZoneId;

@Controller
    @AllArgsConstructor
    public class SessionController {
    private static final String MODEL_SESSION = "proyeccion";


        private final SessionRepository sessionRepository;
        private final MovieRepository movieRepository;
        private final RoomRepository roomRepository;
        private final TicketRepository ticketRepository;
        private final SessionManagementService sessionManagementService;

        // Lista de sesiones
        @GetMapping("/sessions")
public String listSessions(Model model) {
        List<Session> proyecciones = sessionRepository.findAllByActiveTrueOrderByStartTimeDescNullsLast();
        model.addAttribute("proyecciones", proyecciones);
        model.addAttribute("proyeccionesCount", (long) proyecciones.size());
    return "sessions/session-list";
}
    // Formulario nueva sesión
    @GetMapping("/sessions/new")
    public String newSession(Model model) {
        model.addAttribute(MODEL_SESSION, new SessionForm());
        model.addAttribute("movies", movieRepository.findAll()); // Todas las películas
        model.addAttribute("rooms", roomRepository.findAll()); // Todas las salas
        return "sessions/session-form";
    }

    // Detalle de sesión
    @GetMapping("/sessions/{id}")
    public String sessionDetail(Model model, @PathVariable Long id) {
        Session session = sessionRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyección no encontrada"));
        Integer capacity = session.getRoom() == null ? null : session.getRoom().getCapacity();
        int seatsPerRow = capacity == null || capacity <= 0 ? 0 : (int) Math.ceil(Math.sqrt(capacity));
        model.addAttribute(MODEL_SESSION, session);
        model.addAttribute("tickets", ticketRepository.findBySession_Id(id)); // Cargar los tickets de esta sesión
        boolean salesOpen = !TicketService.hasSessionStarted(session)
                && Boolean.TRUE.equals(session.getActive())
                && session.getMovie() != null && Boolean.TRUE.equals(session.getMovie().getActive())
                && session.getRoom() != null && Boolean.TRUE.equals(session.getRoom().getActive())
                && session.getPrice() != null && Double.isFinite(session.getPrice())
                && session.getPrice() >= 0;
        model.addAttribute("salesOpen", salesOpen);
        model.addAttribute("seatsPerRow", seatsPerRow);
        return "sessions/session-detail";
    }

    // Formulario editar sesión
    @GetMapping("/sessions/edit/{id}")
    public String editSession(Model model, @PathVariable Long id) {
        model.addAttribute(MODEL_SESSION, SessionForm.from(sessionRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyección no encontrada"))));
        model.addAttribute("movies", movieRepository.findAll());
        model.addAttribute("rooms", roomRepository.findAll());
        return "sessions/session-form";
    }

    // POST: Guardar (crear o actualizar)
    @PostMapping("/sessions")
    public String saveSession(@Valid @ModelAttribute(MODEL_SESSION) SessionForm form,
                              BindingResult bindingResult, Model model) {
        if (form.getId() == null && (form.getStartTime() == null
                || !form.getStartTime().isAfter(LocalDateTime.now(ZoneId.of("Europe/Madrid"))))) {
            bindingResult.rejectValue("startTime", "future", "La fecha de la proyección debe ser futura");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("movies", movieRepository.findAll());
            model.addAttribute("rooms", roomRepository.findAll());
            return "sessions/session-form";
        }
        sessionManagementService.save(form);
        return "redirect:/sessions";
    }

        // GET: Desactivar (en lugar de eliminar, para evitar errores de constraint con tickets asociados)
    @PostMapping("/sessions/deactivate/{id}")
    public String deactivateSession(@PathVariable Long id) {
        sessionManagementService.deactivate(id);
        return "redirect:/sessions";
    }
}
package com.demo.controller;

    import com.demo.model.Session;
    import com.demo.dto.SessionForm;
    import com.demo.repository.SessionRepository;
    import com.demo.repository.MovieRepository;
    import com.demo.repository.RoomRepository;
    import com.demo.repository.TicketRepository;
    import com.demo.service.SessionManagementService;
    import lombok.AllArgsConstructor;
    import jakarta.validation.Valid;
    import org.springframework.validation.BindingResult;
    import org.springframework.stereotype.Controller;
    import org.springframework.ui.Model;
    import org.springframework.web.bind.annotation.GetMapping;
    import org.springframework.web.bind.annotation.ModelAttribute;
    import org.springframework.web.bind.annotation.PathVariable;
    import org.springframework.web.bind.annotation.PostMapping;
    import java.util.List;

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
        int capacity = sessionRepository.findById(id).orElseThrow().getRoom().getCapacity();
        int seatsPerRow = (int) Math.ceil(Math.sqrt(capacity));
        model.addAttribute(MODEL_SESSION, sessionRepository.findById(id).orElseThrow());
        model.addAttribute("tickets", ticketRepository.findBySession_Id(id)); // Cargar los tickets de esta sesión
        model.addAttribute("seatsPerRow", seatsPerRow);
        return "sessions/session-detail";
    }

    // Formulario editar sesión
    @GetMapping("/sessions/edit/{id}")
    public String editSession(Model model, @PathVariable Long id) {
        model.addAttribute(MODEL_SESSION, SessionForm.from(sessionRepository.findById(id).orElseThrow()));
        model.addAttribute("movies", movieRepository.findAll());
        model.addAttribute("rooms", roomRepository.findAll());
        return "sessions/session-form";
    }

    // POST: Guardar (crear o actualizar)
    @PostMapping("/sessions")
    public String saveSession(@Valid @ModelAttribute(MODEL_SESSION) SessionForm form,
                              BindingResult bindingResult, Model model) {
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
        sessionRepository.findById(id).ifPresent(session -> {
            session.setActive(false);
            sessionRepository.save(session);
        });
        return "redirect:/sessions";
    }
}
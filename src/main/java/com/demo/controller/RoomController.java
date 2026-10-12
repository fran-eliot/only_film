package com.demo.controller;

import com.demo.model.Room;
import com.demo.dto.RoomForm;
import com.demo.repository.RoomRepository;
import com.demo.repository.SessionRepository;
import org.springframework.validation.FieldError;
import java.util.Objects;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;

import java.util.Optional;

@Controller
@AllArgsConstructor
public class RoomController {
    private static final String VIEW_ROOM_FORM = "rooms/room-form";

    private static final String REDIRECT_ROOMS = "redirect:/salas";


    //repositorio de room (salas)
    private final RoomRepository roomRepository;
    private final SessionRepository sessionRepository;

    @GetMapping("salas")
    public String rooms(Model model) {
        //model donde se cargan los datos
        model.addAttribute("rooms", roomRepository.findAllByActiveTrue());
        return "rooms/room-list";
    }

    @GetMapping("salas/{id}")
    public String roomDetail(Model model, @PathVariable Long id) {
        Optional<Room> roomOptional = roomRepository.findByIdAndActiveTrue(id);

        if (roomOptional.isPresent()) {
            model.addAttribute("room", roomOptional.get());
            return "rooms/room-detail";
        } else {
            // Redirect para que /salas cargue correctamente el modelo con "rooms"
            // Si devolviéramos "rooms/room-list" directamente sin añadir "rooms" al modelo,
            // Thymeleaf lanzaría un error al intentar hacer #lists.size(rooms) con null
            return REDIRECT_ROOMS;
        }
    }

    @PostMapping("salas/deactivate/{id}")
    public String deactivateSala(@PathVariable Long id, Model model) {

        roomRepository.findById(id).ifPresent(room -> {
            room.setActive(false);
            roomRepository.save(room);
        });
        return REDIRECT_ROOMS;
    }


    @GetMapping ("salas/new")
    public String create(Model model){
        model.addAttribute("room", new RoomForm());
        return VIEW_ROOM_FORM;
    }

    @PostMapping("salas")
    public String guardar(@Valid @ModelAttribute("room") RoomForm form, BindingResult bindingresult) {
        if (form.getId() != null) {
            Room current = roomRepository.findById(form.getId()).orElseThrow();
            if (!Objects.equals(current.getCapacity(), form.getCapacity())
                    && sessionRepository.existsByRoom_Id(current.getId())) {
                bindingresult.addError(new FieldError("room", "capacity", form.getCapacity(), false,
                        null, null, "No se puede cambiar el aforo de una sala con sesiones asociadas"));
            }
        }
        if (bindingresult.hasErrors()) {
            return VIEW_ROOM_FORM;
        }
        Room room = form.getId() == null
                ? new Room()
                : roomRepository.findById(form.getId()).orElseThrow();
        room.setName(form.getName());
        room.setCapacity(form.getCapacity());
        room.setScreenType(form.getScreenType() == null
                ? com.demo.model.enums.ScreenType.STANDARD : form.getScreenType());
        room.setFloorNumber(form.getFloorNumber());
        // Preserve the persisted active flag; never accept the hidden form field.
        if (form.getId() == null) room.setActive(true);
        roomRepository.save(room);
        return REDIRECT_ROOMS;
    }

    @GetMapping ("salas/edit/{id}")
    public String edit(Model model, @PathVariable Long id){
        model.addAttribute("room", RoomForm.from(roomRepository.findById(id).orElseThrow()));
        return VIEW_ROOM_FORM;
    }

}

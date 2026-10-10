package com.demo.controller;

import com.demo.model.Room;
import com.demo.repository.RoomRepository;
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

    //repositorio de room (salas)
    private final RoomRepository roomRepository;

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
            return "redirect:/salas";
        }
    }

    @PostMapping("salas/deactivate/{id}")
    public String deactivateSala(@PathVariable Long id, Model model) {

        // forma 1:
//        Optional<Restaurant> restaurantOptional = restaurantRepository.findById(id);
//        if (restaurantOptional.isPresent()) {
//            Restaurant restaurant = restaurantOptional.get();
//            restaurant.setActive(false);
//            restaurantRepository.save(restaurant);
//        }
//        return "redirect:/restaurants";

        // forma 2 (opcional):
        roomRepository.findById(id).ifPresent(room -> {
            room.setActive(false);
            roomRepository.save(room);
        });
        return "redirect:/salas";
    }


    @GetMapping ("salas/new")
    public String create(Model model){
        model.addAttribute("room", new Room());
        return "rooms/room-form";
    }

    @PostMapping("salas")
    public String guardar(@Valid @ModelAttribute Room room, BindingResult bindingresult){
        if (bindingresult.hasErrors()){
            return "rooms/room-form";
        }
        roomRepository.save(room);
        return "redirect:/salas";
    }

    @GetMapping ("salas/edit/{id}")
    public String edit(Model model, @PathVariable Long id){
        model.addAttribute("room", roomRepository.findById(id).orElseThrow());
        return "rooms/room-form";
    }

}

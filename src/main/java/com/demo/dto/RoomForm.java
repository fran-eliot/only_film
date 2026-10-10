package com.demo.dto;

import com.demo.model.Room;
import com.demo.model.enums.ScreenType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomForm {
    private Long id;
    @NotBlank(message = "El nombre de la sala es obligatorio")
    @Size(min = 3, max = 50)
    private String name;
    @NotNull
    @Min(10) @Max(300)
    private Integer capacity;
    private ScreenType screenType = ScreenType.STANDARD;
    @Min(0)
    private Integer floorNumber;
    // For template compatibility only. Never applied to the saved entity.
    private Boolean active;

    public static RoomForm from(Room room) {
        RoomForm form = new RoomForm();
        form.id = room.getId();
        form.name = room.getName();
        form.capacity = room.getCapacity();
        form.screenType = room.getScreenType();
        form.floorNumber = room.getFloorNumber();
        form.active = room.getActive();
        return form;
    }
}

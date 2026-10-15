package com.demo.model;


import com.demo.model.enums.ScreenType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name="salas")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    //nombre de la sala (Sala 1...)
    @Column(nullable = false)
    @NotBlank(message = "El nombre de la sala es obligatorio")
    @Size(min = 3, max = 50,
            message = "El nombre debe tener entre 3 y 50 caracteres")
    private String name;

    //número máximo de asientos disponibles en la sala
    @Column(nullable = false)
    @Min(value = 10, message = "La capacidad mínima es 10 asientos")
    @Max(value = 300, message = "La capacidad máxima es 300 asientos")
    private Integer capacity;

    //tipo de sala (IMAX, STANDARD, 3D, 4DX...)
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ScreenType screenType = ScreenType.STANDARD;

    @Column(columnDefinition = "BOOLEAN DEFAULT true") //por defecto será true cuando se inserta en base de datos
    @Builder.Default
    private Boolean active = true;
    // private Boolean active = true; Esto inicializa el atributo cuando creas el objeto:
    // Room room = new Room();

    @Min(value = 0, message = "El número de planta no puede ser negativo")
    private Integer floorNumber;

}

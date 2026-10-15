package com.demo.model;


import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Table(name = "sessions")
public class Session {
    @Id
    //identificador único para cada sesión.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // fecha y hora de inicio de la sesión, se asigna automáticamente al crear una nueva sesión.
    @Builder.Default
    private LocalDateTime startTime = LocalDateTime.now(ZoneId.of("Europe/Madrid"));

     //asociación con la película que se proyectará en esta sesión.

    //precio de la entrada para esta sesión (12.50, 15.00, etc.)
    private Double price;

    //idioma ("VO", "doblada", "VOSE") (Podría ser enum)
    private String language;

    //Integer numAds o Integer adMinutes o Duration adMinutes
    private Integer adMinutes;

    @Builder.Default
    @Column(columnDefinition = "BOOLEAN DEFAULT true")
    private Boolean active = true;

    //asociación con la película que se proyectará en esta sesión.
    @ManyToOne
    @ToString.Exclude
    private Movie movie;

    @ManyToOne
    @ToString.Exclude
    private Room room;

}

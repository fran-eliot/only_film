package com.demo.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name="reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    //título de la review
    private String title;

    //texto de la review
    @Column(length = 1000)
    private String description;

    //rating, de cinco
    private Integer rating;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Builder.Default // para que el builder no ponga este campo a null
    private LocalDateTime creationDate = LocalDateTime.now(ZoneId.systemDefault());

    //película
    @ManyToOne
    private Movie movie;

    // Usuario que escribió la reseña
    @ManyToOne
    private User user;
}
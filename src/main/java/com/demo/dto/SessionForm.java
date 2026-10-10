package com.demo.dto;

import com.demo.model.Session;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
public class SessionForm {
    private Long id;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startTime;
    private Double price;
    private String language;
    private Integer adMinutes;
    private Long movieId;
    private Long roomId;

    public static SessionForm from(Session session) {
        SessionForm form = new SessionForm();
        form.id = session.getId();
        form.startTime = session.getStartTime();
        form.price = session.getPrice();
        form.language = session.getLanguage();
        form.adMinutes = session.getAdMinutes();
        form.movieId = session.getMovie() == null ? null : session.getMovie().getId();
        form.roomId = session.getRoom() == null ? null : session.getRoom().getId();
        return form;
    }
}

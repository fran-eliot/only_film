package com.demo.controller;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Translate expected domain/input failures into stable HTTP responses.
 * Security's AccessDeniedException and unexpected programming failures remain
 * with their dedicated framework handlers (403 and 500 respectively).
 */
@ControllerAdvice
public class MvcExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    public void resourceNotFound(HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.NOT_FOUND.value(), "Recurso no encontrado");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public void invalidRequest(HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.BAD_REQUEST.value(), "Datos de solicitud no válidos");
    }

    @ExceptionHandler(IllegalStateException.class)
    public void operationConflict(HttpServletResponse response) throws IOException {
        response.sendError(HttpStatus.CONFLICT.value(), "Operación no disponible en el estado actual");
    }
}

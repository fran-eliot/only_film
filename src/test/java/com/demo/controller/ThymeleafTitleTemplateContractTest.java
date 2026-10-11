package com.demo.controller;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ThymeleafTitleTemplateContractTest {

    private static String template(String path) throws IOException {
        try (var stream = new ClassPathResource("templates/" + path).getInputStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void sharedHeadFragmentDefinesDynamicTitle() throws IOException {
        String head = template("layout/head.html");
        assertTrue(head.contains("th:fragment=\"head(title)\""));
        assertTrue(head.contains("<title th:text=\"${title}\">Only Film</title>"));
    }

    @Test
    void representativePageTypesUseSharedHeadFragment() throws IOException {
        for (String page : new String[] {
                "auth/login.html",
                "error/404.html",
                "tickets/ticket-checkout.html",
                "users/user-list.html"
        }) {
            assertTrue(template(page).contains("th:replace=\"~{layout/head :: head("),
                    () -> "Missing shared head fragment in " + page);
        }
    }

    @Test
    void userTableHasAccessibleCaptionAndColumnHeaders() throws IOException {
        String html = template("users/user-list.html");
        assertTrue(html.contains("<caption class=\"visually-hidden\">Usuarios del sistema</caption>"));
        for (String column : new String[] {"Id", "Username", "Email", "Rol", "Activo", "Acciones"}) {
            assertTrue(html.contains("<th scope=\"col\">" + column + "</th>"),
                    () -> "Missing scoped header: " + column);
        }
    }
    @Test
    void roomAndSessionTablesHaveAccessibleCaptions() throws IOException {
        for (String[] table : new String[][] {
                {"rooms/room-list.html", "Salas del cine"},
                {"sessions/session-list.html", "Proyecciones del cine"}
        }) {
            String html = template(table[0]);
            assertTrue(html.contains("<caption class=\"visually-hidden\">" + table[1] + "</caption>"),
                    () -> "Missing accessible table caption in " + table[0]);
        }
    }

    @Test
    void profilePasswordFieldReferencesItsHelpText() throws IOException {
        String html = template("users/profile-form.html");
        assertTrue(html.contains("aria-describedby=\"passwordHelp\""));
        assertTrue(html.contains("<small id=\"passwordHelp\""));
        assertTrue(html.contains("vacío para conservar la misma contraseña."));
    }

    @Test
    void registrationFieldErrorsAreAssociatedWithTheirInputs() throws IOException {
        String html = template("auth/register.html");
        for (String field : new String[] {
                "username", "firstName", "lastName", "email", "password", "passwordConfirm"
        }) {
            String condition = "#fields.hasErrors('" + field + "')";
            assertTrue(html.contains("th:aria-invalid=\"${" + condition + "}\""),
                    () -> "Missing invalid state for " + field);
            assertTrue(html.contains("th:aria-describedby=\"${" + condition + " ? '" + field + "Error' : null}\""),
                    () -> "Missing error association for " + field);
            assertTrue(html.contains("id=\"" + field + "Error\" class=\"invalid-feedback\""),
                    () -> "Missing feedback id for " + field);
        }
    }

    @Test
    void authenticationMessagesHaveAccessibleRoles() throws IOException {
        String login = template("auth/login.html");
        assertTrue(login.contains("th:if=\"${param.error}\" class=\"alert alert-danger\" role=\"alert\""));
        assertTrue(login.contains("th:if=\"${param.logout}\" class=\"alert alert-success\" role=\"status\""));
        assertTrue(login.contains("th:if=\"${message}\" class=\"alert alert-success\" role=\"status\""));

        String register = template("auth/register.html");
        assertTrue(register.contains("th:if=\"${error}\" class=\"alert alert-danger\" role=\"alert\""));
    }
    @Test
    void userFormErrorsHaveAccessibleAlertRoles() throws IOException {
        for (String path : new String[] {"users/profile-form.html", "users/user-form.html"}) {
            assertTrue(template(path).contains("th:if=\"${error}\" class=\"alert alert-danger\" role=\"alert\""),
                    () -> "Missing error alert role in " + path);
        }
    }
    @Test
    void userListFeedbackUsesAccessibleRoles() throws IOException {
        String html = template("users/user-list.html");
        assertTrue(html.contains("th:if=\"${message}\" class=\"alert alert-success mt-3\" role=\"status\""));
        assertTrue(html.contains("th:if=\"${error}\"   class=\"alert alert-danger  mt-3\" role=\"alert\""));
    }
    @Test
    void missingReviewRatingAnnouncesErrorAndFocusesFirstStar() throws IOException {
        String html = template("reviews/review-form.html");
        assertTrue(html.contains("id=\"starRating\" aria-describedby=\"ratingText ratingError\""));
        assertTrue(html.contains("id=\"ratingError\" role=\"alert\""));
        assertTrue(html.contains("ratingError.style.display = 'block';\n            stars[0].focus();"));
    }
}

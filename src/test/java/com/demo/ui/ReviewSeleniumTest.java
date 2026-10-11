package com.demo.ui;

import com.demo.model.Movie;
import com.demo.model.Room;
import com.demo.model.Session;
import com.demo.model.Ticket;
import com.demo.model.User;
import com.demo.model.enums.BuyStatus;
import com.demo.model.enums.Role;
import com.demo.model.enums.ScreenType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ReviewSeleniumTest extends BaseSeleniumTest {

    private User reviewUser;
    private Movie movieToReview;
    private Room room;
    private Session session;
    private Ticket paidTicket;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUpReview() {
        ticketRepository.deleteAll();
        reviewRepository.deleteAll();
        sessionRepository.deleteAll();
        roomRepository.deleteAll();
        movieRepository.deleteAll();
        userRepository.deleteAll();

        wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // Crear usuario que escribirá la reseña
        reviewUser = userRepository.save(User.builder()
                .username("review_user")
                .email("review_user@onlyfilm.com")
                .firstName("Review")
                .lastName("User")
                .password(passwordEncoder.encode("12341234"))
                .role(Role.ROLE_USER)
                .active(true)
                .build());

        // Crear película a reseñar
        movieToReview = movieRepository.save(Movie.builder()
                .title("Inception")
                .director("Christopher Nolan")
                .genre("Ciencia ficción")
                .durationMinutes(148)
                .releaseYear(2010)
                .imageUrl("/images/OnlyFilm.png")
                .active(true)
                .build());

        // Crear sala
        room = roomRepository.save(Room.builder()
                .name("Sala 3D")
                .capacity(20)
                .screenType(ScreenType.D3)
                .floorNumber(2)
                .active(true)
                .build());

        // Crear sesión
        session = sessionRepository.save(Session.builder()
                .movie(movieToReview)
                .room(room)
                .startTime(LocalDateTime.now().plusDays(1).withHour(19).withMinute(0))
                .price(12.50)
                .language("VOSE")
                .adMinutes(15)
                .build());

        // Crear entrada pagada para que el usuario pueda escribir reseña
        paidTicket = ticketRepository.save(Ticket.builder()
                .session(session)
                .user(reviewUser)
                .row("C")
                .seat("5")
                .price(12.50)
                .discount(0.0)
                .status(BuyStatus.PAGADO)
                .qrCode("REVIEW-TEST-QR")
                .build());
    }

    @Test
    @DisplayName("Usuario escribe una reseña de una película")
    void userWritesReviewSuccessfully() throws Exception {
        // Iniciar sesión como usuario de review
        loginReviewUser();

        // Navegar a la película
        driver.get(baseUrl + "movies/" + movieToReview.getId());
        waitForPageLoad();

        assertTrue(driver.getPageSource().contains(movieToReview.getTitle()));
        assertTrue(driver.getPageSource().contains("Inception"));

        // Buscar y hacer click en el botón de escribir reseña (ahora con ID)
        WebElement writeReviewButton = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.id("writeReviewBtn"))
        );

        scrollToViewAndClick(writeReviewButton);

        // Verificar que estamos en el formulario de reseña
        wait.until(ExpectedConditions.urlContains("/reviews/new?movieId=" + movieToReview.getId()));
        waitForPageLoad();

        // Verificar que el formulario está presente
        assertTrue(driver.getPageSource().contains("Nueva reseña"));
        assertTrue(driver.getPageSource().contains(movieToReview.getTitle()));

        // Rellenar el formulario
        String reviewTitle = "Una obra maestra del cine";
        String reviewDescription = "Inception es una película extraordinaria que te mantiene al borde del asiento. "
                + "La cinematografía es impresionante, la trama es compleja pero bien ejecutada, y los actores "
                + "realizan un trabajo excepcional. Es una película que definitivamente recomendaría a cualquiera "
                + "que le guste el cine de ciencia ficción.";

        // Llenar el campo de título
        WebElement titleInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("title")));
        titleInput.clear();
        titleInput.sendKeys(reviewTitle);

        // Llenar el campo de descripción
        WebElement descriptionInput = driver.findElement(By.id("description"));
        descriptionInput.clear();
        descriptionInput.sendKeys(reviewDescription);

        // Hacer click en la cuarta estrella para dar 4/5
        List<WebElement> stars = wait.until(
                ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("#starRating i"))
        );
        assertEquals(5, stars.size(), "Debería haber 5 estrellas");

        clickStarSafely(stars.get(3));

        // Verificar que la puntuación se ha actualizado
        wait.until(ExpectedConditions.textToBePresentInElement(
                driver.findElement(By.id("ratingText")), "4 / 5"
        ));

        // Submit del formulario (ahora con ID)
        WebElement submitButton = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.id("submitReviewBtn"))
        );

        scrollToViewAndClick(submitButton);

        // Verificar que la reseña se guardó y fue redirigido a la película con hashtag #reviews
        wait.until(ExpectedConditions.urlMatches(".*/movies/\\d+.*"));
        waitForPageLoad();

        // Verificar que aparezca la reseña en la página de la película
        assertTrue(driver.getPageSource().contains(reviewTitle));
        assertTrue(driver.getPageSource().contains(reviewDescription));
    }

    @Test
    @DisplayName("Usuario no puede escribir reseña sin seleccionar puntuación")
    void userCannotSubmitReviewWithoutRating() throws Exception {
        loginReviewUser();

        driver.get(baseUrl + "reviews/new?movieId=" + movieToReview.getId());
        waitForPageLoad();

        // Rellenar solo el título y descripción (sin estrella)
        WebElement titleInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("title")));
        titleInput.sendKeys("Un buen título");

        WebElement descriptionInput = driver.findElement(By.id("description"));
        descriptionInput.sendKeys("Una descripción aunque sea corta");

        // Intentar enviar sin click en estrellas (ahora con ID)
        WebElement submitButton = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.id("submitReviewBtn"))
        );

        scrollToViewAndClick(submitButton);

        // Verificar que el error se muestra
        WebElement ratingError = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("ratingError"))
        );

        assertTrue(ratingError.isDisplayed());
        assertTrue(ratingError.getText().contains("selecciona una puntuación"));
    }

    @Test
    @DisplayName("Usuario puede navegar al formulario de reseña desde la película")
    void userCanAccessReviewFormFromMovieDetail() {
        loginReviewUser();

        driver.get(baseUrl + "movies/" + movieToReview.getId());
        waitForPageLoad();

        // Debe haber un botón para escribir reseña (ahora con ID)
        WebElement writeReviewButton = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.id("writeReviewBtn"))
        );

        assertTrue(writeReviewButton.isDisplayed());

        scrollToViewAndClick(writeReviewButton);

        wait.until(ExpectedConditions.urlContains("/reviews/new?movieId=" + movieToReview.getId()));
        waitForPageLoad();

        // Verificar que estamos en el formulario de reseña
        WebElement form = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.id("reviewForm"))
        );

        assertTrue(form.isDisplayed());
        assertTrue(driver.getPageSource().contains("Nueva reseña"));
    }

    @Test
    @DisplayName("Las estrellas de puntuación se actualizan correctamente")
    void starRatingUpdatesCorrectly() {
        loginReviewUser();

        driver.get(baseUrl + "reviews/new?movieId=" + movieToReview.getId());
        waitForPageLoad();

        List<WebElement> stars = wait.until(
                ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("#starRating i"))
        );

        // Hacer click en cada estrella y verificar el cambio
        for (int i = 0; i < stars.size(); i++) {
            clickStarSafely(stars.get(i));

            String ratingText = driver.findElement(By.id("ratingText")).getText();
            int expectedRating = i + 1;

            assertTrue(ratingText.contains(expectedRating + " / 5"),
                    "La puntuación debería ser " + expectedRating + " / 5");
        }
    }

    private void loginReviewUser() {
        login("review_user", "12341234");
    }

    private void login(String username, String password) {
        WebElement submitButton = prepareLoginForm(username, password);
        waitForPageLoad();
        scrollToViewAndClick(submitButton);

        wait.until(ExpectedConditions.not(
                ExpectedConditions.urlContains("/login")
        ));

        waitForPageLoad();
    }

    private void scrollToViewAndClick(WebElement element) {
        // Scroll el elemento a la vista
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({behavior: 'instant', block: 'center'});",
                element
        );

        clickWithInterceptFallback(element);
    }

    private void clickStarSafely(WebElement star) {
        // Las estrellas están dentro de la pantalla del formulario
        clickWithInterceptFallback(star);
    }

    private void waitForPageLoad() {
        // Esperar la carga completa del documento y las peticiones jQuery, si existe.
        wait.until(driver -> Boolean.TRUE.equals(((JavascriptExecutor) driver).executeScript(
                "return document.readyState === 'complete' " +
                "&& (typeof jQuery === 'undefined' || jQuery.active === 0);"
        )));
    }
}



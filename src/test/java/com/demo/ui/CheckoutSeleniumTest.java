package com.demo.ui;

import com.demo.model.*;
import com.demo.model.enums.BuyStatus;
import com.demo.model.enums.Role;
import com.demo.model.enums.ScreenType;
import com.demo.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CheckoutSeleniumTest extends BaseSeleniumTest {

    @Autowired
    UserRepository userRepository;

    @Autowired
    MovieRepository movieRepository;

    @Autowired
    RoomRepository roomRepository;

    @Autowired
    SessionRepository sessionRepository;

    @Autowired
    TicketRepository ticketRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    User testUser;
    Movie movie;
    Room room;
    Session session;
    Ticket freeTicket;

    @BeforeEach
    void setUpCheckout() {
        ticketRepository.deleteAll();
        sessionRepository.deleteAll();
        roomRepository.deleteAll();
        movieRepository.deleteAll();
        userRepository.deleteAll();

        wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        testUser = userRepository.save(User.builder()
                .username("checkout_user")
                .email("checkout@onlyfilm.com")
                .firstName("Checkout")
                .lastName("User")
                .password(passwordEncoder.encode("password123"))
                .role(Role.ROLE_USER)
                .active(true)
                .build());

        movie = movieRepository.save(Movie.builder()
                .title("Interstellar")
                .director("Christopher Nolan")
                .genre("Ciencia ficción")
                .durationMinutes(169)
                .releaseYear(2014)
                .imageUrl("/images/OnlyFilm.png")
                .active(true)
                .build());

        room = roomRepository.save(Room.builder()
                .name("Sala Selenium")
                .capacity(10)
                .screenType(ScreenType.STANDARD)
                .floorNumber(1)
                .active(true)
                .build());

        session = sessionRepository.save(Session.builder()
                .movie(movie)
                .room(room)
                .startTime(LocalDateTime.now().plusDays(1))
                .price(9.50)
                .language("VOSE")
                .adMinutes(10)
                .build());

        freeTicket = ticketRepository.save(Ticket.builder()
                .session(session)
                .row("A")
                .seat("1")
                .price(session.getPrice())
                .discount(0.0)
                .status(BuyStatus.LIBRE)
                .build());

        ticketRepository.saveAll(List.of(
                Ticket.builder()
                        .session(session)
                        .row("A")
                        .seat("2")
                        .price(9.50)
                        .discount(0.0)
                        .status(BuyStatus.LIBRE)
                        .build(),

                Ticket.builder()
                        .session(session)
                        .row("A")
                        .seat("3")
                        .price(9.50)
                        .discount(0.0)
                        .status(BuyStatus.LIBRE)
                        .build(),

                Ticket.builder()
                        .session(session)
                        .row("A")
                        .seat("4")
                        .price(9.50)
                        .discount(0.0)
                        .status(BuyStatus.PAGADO)
                        .build()
        ));
    }

    @Test
    void checkoutCompleteFlow() {
        login();

        driver.get(baseUrl + "sessions/" + session.getId());

        WebElement freeSeat = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector(".seat-btn.free"))
        );

        clickSafely(freeSeat);

        WebElement modal = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("confirmarCompraModal"))
        );

        assertTrue(modal.isDisplayed());

        assertEquals("A", driver.findElement(By.id("modal-row")).getText());
        assertEquals("1", driver.findElement(By.id("modal-seat")).getText());

        WebElement continueButton = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.id("btn-confirmar"))
        );

        clickSafely(continueButton);

        wait.until(ExpectedConditions.urlToBe(
                baseUrl + "tickets/" + freeTicket.getId() + "/checkout"
        ));

        assertTrue(driver.getPageSource().contains("Interstellar"));
        assertTrue(driver.getPageSource().contains("Sala Selenium"));
        assertTrue(driver.getPageSource().contains("Fila"));
        assertTrue(driver.getPageSource().contains("Butaca"));

        WebElement firstSnackButton = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector(".snack-toggle"))
        );

        clickSafely(firstSnackButton);

        wait.until(ExpectedConditions.textToBePresentInElement(firstSnackButton, "Añadido"));

        assertTrue(firstSnackButton.getText().contains("Añadido"));

        driver.findElement(By.id("cardHolder")).sendKeys("Checkout User");
        driver.findElement(By.id("cardNumber")).sendKeys("4242424242424242");
        driver.findElement(By.id("cardExpiry")).sendKeys("12/30");
        driver.findElement(By.id("cardCvv")).sendKeys("123");

        WebElement payButton = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.id("pay-btn"))
        );

        clickSafely(payButton);

        wait.until(ExpectedConditions.urlToBe(
                baseUrl + "tickets/" + freeTicket.getId()
        ));

        String page = driver.getPageSource();

        assertTrue(page.contains("Interstellar"));
        assertTrue(page.contains("Sala Selenium"));
        assertTrue(page.contains("Fila"));
        assertTrue(page.contains("A"));
        assertTrue(page.contains("Asiento"));
        assertTrue(page.contains("1"));
        assertTrue(page.contains("ONLYFILM-"));
        assertTrue(page.contains("Código de acceso"));

        Ticket paidTicket = ticketRepository.findById(freeTicket.getId()).orElseThrow();

        assertEquals(BuyStatus.PAGADO, paidTicket.getStatus());
        assertEquals(testUser.getId(), paidTicket.getUser().getId());
        assertNotNull(paidTicket.getQrCode());
        assertTrue(paidTicket.getQrCode().startsWith("ONLYFILM-"));
        assertEquals(4.50, paidTicket.getSnackPrice());
    }

    private void login() {
        driver.get(baseUrl + "login");

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")))
                .sendKeys("checkout_user");

        driver.findElement(By.id("password"))
                .sendKeys("password123");

        WebElement loginButton = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("button[type='submit']"))
        );

        clickSafely(loginButton);

        wait.until(ExpectedConditions.not(
                ExpectedConditions.urlContains("/login")
        ));
    }

    private void clickSafely(WebElement element) {
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block: 'center', inline: 'center'});",
                element
        );

        try {
            wait.until(ExpectedConditions.elementToBeClickable(element));
            element.click();
        } catch (ElementClickInterceptedException _) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
        }
    }
}

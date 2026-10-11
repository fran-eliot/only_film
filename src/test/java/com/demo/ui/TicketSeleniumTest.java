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
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
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

public class TicketSeleniumTest extends BaseSeleniumTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User user;
    private User admin;
    private Movie movie;
    private Room room;
    private Session session;
    private Ticket paidTicket;
    private Ticket freeTicket;

    @BeforeEach
    void setUpTickets() {
        ticketRepository.deleteAll();
        reviewRepository.deleteAll();
        sessionRepository.deleteAll();
        roomRepository.deleteAll();
        movieRepository.deleteAll();
        userRepository.deleteAll();

        wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        user = userRepository.save(User.builder()
                .username("ticket_user")
                .email("ticket_user@onlyfilm.com")
                .firstName("Ticket")
                .lastName("User")
                .password(passwordEncoder.encode("password123"))
                .role(Role.ROLE_USER)
                .active(true)
                .build());

        admin = userRepository.save(User.builder()
                .username("admin_ticket")
                .email("admin_ticket@onlyfilm.com")
                .firstName("Admin")
                .lastName("Ticket")
                .password(passwordEncoder.encode("admin123"))
                .role(Role.ROLE_ADMIN)
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
                .startTime(LocalDateTime.now().plusDays(1).withHour(20).withMinute(30))
                .price(9.50)
                .language("VOSE")
                .adMinutes(10)
                .build());

        paidTicket = ticketRepository.save(Ticket.builder()
                .session(session)
                .user(user)
                .row("A")
                .seat("1")
                .price(9.50)
                .discount(0.0)
                .snackPrice(4.50)
                .status(BuyStatus.PAGADO)
                .qrCode("ONLYFILM-TEST-QR")
                .build());

        freeTicket = ticketRepository.save(Ticket.builder()
                .session(session)
                .row("B")
                .seat("2")
                .price(9.50)
                .discount(0.0)
                .status(BuyStatus.LIBRE)
                .build());
    }

    @Test
    void ticketListAsUserShowsOnlyUserTickets() {
        loginUser();

        driver.get(baseUrl + "tickets");

//        assertEquals("Mis Tickets", driver.findElement(By.tagName("h1")).getText());

        assertTrue(driver.getPageSource().contains(movie.getTitle()));
        assertTrue(driver.getPageSource().contains("Fila: " + paidTicket.getRow()));
        assertTrue(driver.getPageSource().contains("Asiento: " + paidTicket.getSeat()));
        assertTrue(driver.getPageSource().contains(room.getName()));
        assertTrue(driver.getPageSource().contains(BuyStatus.PAGADO.name()));

        By detailLink = By.cssSelector("a[href='/tickets/" + paidTicket.getId() + "']");
        WebElement detailButton = wait.until(ExpectedConditions.elementToBeClickable(detailLink));
        assertEquals("Detalles del Ticket", detailButton.getText().trim());
        detailButton.click();

        // Esperar a que la navegación termine antes de comprobar la URL
        wait.until(ExpectedConditions.urlToBe(baseUrl + "tickets/" + paidTicket.getId()));
        assertEquals(baseUrl + "tickets/" + paidTicket.getId(), driver.getCurrentUrl());
    }

    @Test
    void ticketDetailShowsPaidTicketInformationAndQr() {
        loginUser();

        driver.get(baseUrl + "tickets/" + paidTicket.getId());

        assertTrue(driver.getPageSource().contains(movie.getTitle()));
        assertTrue(driver.getPageSource().contains(room.getName()));
        assertTrue(driver.getPageSource().contains("PAGADO"));
        assertTrue(driver.getPageSource().contains("Fila"));
        assertTrue(driver.getPageSource().contains(paidTicket.getRow()));
        assertTrue(driver.getPageSource().contains("Asiento"));
        assertTrue(driver.getPageSource().contains(paidTicket.getSeat()));
        assertTrue(driver.getPageSource().contains(user.getEmail()));
        assertTrue(driver.getPageSource().contains("ONLYFILM-TEST-QR"));
        assertTrue(driver.getPageSource().contains("Código de acceso"));

        WebElement qrImage = driver.findElement(By.cssSelector(".ticket-qr-img"));
        assertTrue(qrImage.getAttribute("src").contains("ONLYFILM-TEST-QR"));
    }

    @Test
    void checkoutTemplateShowsTicketSnacksAndPaymentForm() {
        loginUser();

        driver.get(baseUrl + "tickets/" + freeTicket.getId() + "/checkout");

        assertTrue(driver.getPageSource().contains("Tu entrada"));
        assertTrue(driver.getPageSource().contains(movie.getTitle()));
        assertTrue(driver.getPageSource().contains(room.getName()));
        assertTrue(driver.getPageSource().contains("Fila"));
        assertTrue(driver.getPageSource().contains(freeTicket.getRow()));
        assertTrue(driver.getPageSource().contains("Butaca"));
        assertTrue(driver.getPageSource().contains(freeTicket.getSeat()));

        assertTrue(driver.getPageSource().contains("¿Añadir algo de picar?"));
        assertTrue(driver.getPageSource().contains("Palomitas medianas"));
        assertTrue(driver.getPageSource().contains("Palomitas grandes"));
        assertTrue(driver.getPageSource().contains("Refresco"));
        assertTrue(driver.getPageSource().contains("Pack duo"));
        assertTrue(driver.getPageSource().contains("Combo familiar"));

        assertNotNull(driver.findElement(By.id("cardHolder")));
        assertNotNull(driver.findElement(By.id("cardNumber")));
        assertNotNull(driver.findElement(By.id("cardExpiry")));
        assertNotNull(driver.findElement(By.id("cardCvv")));
        assertNotNull(driver.findElement(By.id("pay-btn")));
    }

    @Test
    void checkoutAddsSnackAndUpdatesTotals() {
        loginUser();

        driver.get(baseUrl + "tickets/" + freeTicket.getId() + "/checkout");

        WebElement firstSnackButton = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector(".snack-toggle"))
        );

        clickSafely(firstSnackButton);

        wait.until(ExpectedConditions.textToBePresentInElement(firstSnackButton, "Añadido"));

        WebElement palomitasInput = driver.findElement(By.name("qtyPalomitasMedianas"));

        assertEquals("1", palomitasInput.getAttribute("value"));
        assertTrue(firstSnackButton.getText().contains("Añadido"));
        assertTrue(driver.findElement(By.id("snack-subtotal")).getText().contains("4,50"));
        assertTrue(driver.findElement(By.id("pay-snack-subtotal")).getText().contains("4,50"));
        assertTrue(driver.findElement(By.id("total-display")).getText().contains("14,00"));
        assertTrue(driver.findElement(By.id("pay-total-display")).getText().contains("14,00"));
    }

    @Test
    void checkoutCompletePaymentRedirectsToTicketDetail() {
        loginUser();

        driver.get(baseUrl + "tickets/" + freeTicket.getId() + "/checkout");

        WebElement firstSnackButton = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector(".snack-toggle"))
        );

        clickSafely(firstSnackButton);

        wait.until(ExpectedConditions.textToBePresentInElement(firstSnackButton, "Añadido"));

        driver.findElement(By.id("cardHolder")).sendKeys("Ticket User");
        driver.findElement(By.id("cardNumber")).sendKeys("4242424242424242");
        driver.findElement(By.id("cardExpiry")).sendKeys("12/30");
        driver.findElement(By.id("cardCvv")).sendKeys("123");

        WebElement payButton = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.id("pay-btn"))
        );

        clickSafely(payButton);

        wait.until(ExpectedConditions.urlToBe(baseUrl + "tickets/" + freeTicket.getId()));

        String page = driver.getPageSource();

        assertTrue(page.contains("¡Entrada confirmada!"));
        assertTrue(page.contains("Disfruta la película"));
        assertTrue(page.contains(movie.getTitle()));
        assertTrue(page.contains(room.getName()));
        assertTrue(page.contains("PAGADO"));
        assertTrue(page.contains("Código de acceso"));
        assertTrue(page.contains("ONLYFILM-"));

        Ticket ticketDB = ticketRepository.findById(freeTicket.getId()).orElseThrow();

        assertEquals(BuyStatus.PAGADO, ticketDB.getStatus());
        assertEquals(user.getId(), ticketDB.getUser().getId());
        assertNotNull(ticketDB.getQrCode());
        assertTrue(ticketDB.getQrCode().startsWith("ONLYFILM-"));
        assertEquals(4.50, ticketDB.getSnackPrice());
    }

    @Test
    void buyTicketRedirectsToCheckout() {
        loginUser();

        driver.get(baseUrl + "tickets/buy/" + freeTicket.getId());

        wait.until(ExpectedConditions.urlToBe(
                baseUrl + "tickets/" + freeTicket.getId() + "/checkout"
        ));

        assertEquals(baseUrl + "tickets/" + freeTicket.getId() + "/checkout", driver.getCurrentUrl());
        assertTrue(driver.getPageSource().contains("Tu entrada"));
        assertTrue(driver.getPageSource().contains(movie.getTitle()));
    }

    @Test
    void editFreeTicketAsAdminOnlyShowsDiscountField() {
        loginAdmin();

        driver.get(baseUrl + "tickets/edit/" + freeTicket.getId());

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("discount")));
        assertTrue(driver.getPageSource().contains("Editar descuento de entrada libre"));
        assertEquals("0", driver.findElement(By.id("discount")).getAttribute("value").replaceFirst("\\.0$", ""));

        // Los datos de inventario y checkout no pueden alterarse desde el formulario.
        for (String protectedField : List.of("row", "seat", "price", "status", "session", "user", "qrCode")) {
            assertTrue(driver.findElements(By.id(protectedField)).isEmpty(), protectedField);
        }
    }

    private void loginUser() {
        login("ticket_user", "password123");
    }

    private void loginAdmin() {
        login("admin_ticket", "admin123");
    }

    private void login(String username, String password) {
        driver.get(baseUrl + "login");

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")))
                .sendKeys(username);

        driver.findElement(By.id("password")).sendKeys(password);

        WebElement submitButton = wait.until(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("button[type='submit']"))
        );

        clickSafely(submitButton);

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
        } catch (ElementClickInterceptedException e) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
        }
    }
}

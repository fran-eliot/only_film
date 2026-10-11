package com.demo.ui;

import com.demo.model.Movie;
import com.demo.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Map;

//Ponemos un puerto aleatorio para no tener problema con el puerto 8080 que estamos utilizando porque podria fallar el test
@ExtendWith(ScreenshotOnFailure.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseSeleniumTest {
    @LocalServerPort
    int port; //Inyectamos el puerto aleatorio

    @Autowired
    MovieRepository movieRepository;

    @Autowired
    TicketRepository ticketRepository;

    @Autowired
    SessionRepository sessionRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoomRepository roomRepository;

    @Autowired
    ReviewRepository reviewRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    String baseUrl;
    WebDriver driver;
    WebDriverWait wait;

    Movie movieAction;
    Movie movieRomance;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
        reviewRepository.deleteAll();
        sessionRepository.deleteAll();
        roomRepository.deleteAll();
        movieRepository.deleteAll();
        userRepository.deleteAll();

        movieAction = movieRepository.save(
                Movie.builder().active(true).title("The Bourne Identity").genre("Action")
                        .durationMinutes(119).releaseYear(2002).director("Doug Liman").build()
        );
        movieRomance = movieRepository.save(
                Movie.builder().active(true).title("The Age of Adaline").genre("Romance")
                        .durationMinutes(112).releaseYear(2015).director("Lee Toland Krieger").build()
        );

        // inicializar y configuración de driver
        baseUrl = "http://localhost:" + port + "/";

        // options para GitHub Actions
        boolean ci = System.getenv("CI") != null; // GitHub Actions pone CI=True
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("--window-size=1920,1080");
        // Forzar es-ES en el navegador -> Accept-Language es-ES -> el servidor formatea los
        // decimales con coma igual en local y en CI. (El <input type="date"> NO se controla
        // con esto en Linux: usa el locale del SO; por eso su fecha se fija por valor ISO
        // en el propio test, no tecleando.)
        chromeOptions.addArguments("--lang=es-ES");
        chromeOptions.setExperimentalOption("prefs", Map.of("intl.accept_languages", "es-ES"));
        if (ci) {
            chromeOptions.addArguments("--headless=new", "--no-sandbox", "--disable-gpu", "--disable-dev-shm-usage");
        }
        driver = new ChromeDriver(chromeOptions);
        wait = new WebDriverWait(driver, Duration.ofSeconds(30L));
    }

    //Siempre cerramos para que no queden procesos abiertos
    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}


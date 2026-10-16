package com.demo.ui;

import com.demo.model.Movie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class MovieSeleniumTest extends BaseSeleniumTest{
    private Movie movieAction;

    @BeforeEach
    void setUpMovies() {
        movieAction = movieRepository.save(
                Movie.builder().active(true).title("The Bourne Identity").genre("Action")
                        .durationMinutes(119).releaseYear(2002).director("Doug Liman").build()
        );
        movieRepository.save(
                Movie.builder().active(true).title("The Age of Adaline").genre("Romance")
                        .durationMinutes(112).releaseYear(2015).director("Lee Toland Krieger").build()
        );
    }
    @Test
    void movieList(){
        driver.get(baseUrl + "movies");

        //Verificamos el h1 de la página (debemos tener 1 solo h1 por html)
        //el .tagName busca la etiqueta <h1>
        assertTrue(driver.findElement(By.tagName("h1")).getText().contains("Listado de películas"));

        //Verificamos el badge de resultados (ej: "2 resultados")
        //El .cssSelector es una clase de css compuesta
        assertTrue(driver.findElement(By.id("countMovies")).getText().contains("película"));
        assertTrue(driver.findElement(By.id("countMovies")).getText().contains("disponible"));

        //Verifica que hay cards de películas
        //Esta buscando por clase de CSS
        wait.until(ExpectedConditions.presenceOfElementLocated(By.className("movie-card")));
        List<WebElement> cards = driver.findElements(By.className("movie-card"));
        assertFalse(cards.isEmpty());

        //La primera card tiene el nombre de la película que creamos
        WebElement firstCard = cards.getFirst();
        assertTrue(firstCard.getText().contains(movieAction.getTitle()));
        new Actions(driver).moveToElement(firstCard).perform();

        //Al hacer click en el boton más información estamos verificando que navegamos al movie-detail
        //el linkText busca la etiqueta <a> con el texto exacto que le pasamos
        //el getCurrentUrl es la url actual del navegador
        new Actions(driver).moveToElement(driver.findElement(By.cssSelector("a[href='/movies/" + movieAction.getId() + "']"))).click().perform();
        wait.until(driver -> driver.getCurrentUrl().matches(".*/movies/\\d+$"));

    }

    @Test
    void movieDetail() {
        driver.get(baseUrl + "movies/" + movieAction.getId());

        //Estamos comprobando que el título de la película aparece en la card
        //el .cssSelector es la clase compuesta de css
        wait.until(ExpectedConditions.textToBePresentInElementLocated(
                By.tagName("h1"), movieAction.getTitle()));
        String titulo = driver.findElement(By.tagName("h1")).getText();
        assertEquals(movieAction.getTitle(), titulo);

        //Aqui verificamos que el director aparece en la página de movie-detail
        assertTrue(driver.getPageSource().contains(movieAction.getDirector()));
    }

}

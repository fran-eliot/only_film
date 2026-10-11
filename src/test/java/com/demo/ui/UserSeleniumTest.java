package com.demo.ui;

import com.demo.model.User;
import com.demo.model.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class UserSeleniumTest extends BaseSeleniumTest {

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User admin;
    private User regularUser;
    private User inactiveUser;

    @BeforeEach
    void setUpUsers() {
        userRepository.deleteAll();

        admin = userRepository.save(User.builder()
                .username("admin_user_selenium")
                .email("admin.selenium@onlyfilm.com")
                .firstName("Admin")
                .lastName("Selenium")
                .password(passwordEncoder.encode("admin123"))
                .role(Role.ROLE_ADMIN)
                .active(true)
                .build());

        regularUser = userRepository.save(User.builder()
                .username("regular_user_selenium")
                .email("regular.selenium@onlyfilm.com")
                .firstName("Regular")
                .lastName("Selenium")
                .password(passwordEncoder.encode("user123"))
                .role(Role.ROLE_USER)
                .active(true)
                .build());

        inactiveUser = userRepository.save(User.builder()
                .username("inactive_user_selenium")
                .email("inactive.selenium@onlyfilm.com")
                .firstName("Inactive")
                .lastName("Selenium")
                .password(passwordEncoder.encode("inactive123"))
                .role(Role.ROLE_USER)
                .active(false)
                .build());
    }

    @Test
    void adminCanSeeUsersList() {
        loginAdmin();

        driver.get(baseUrl + "admin/users");

        assertTrue(driver.getPageSource().contains("Usuarios"));
        assertTrue(driver.getPageSource().contains(admin.getUsername()));
        assertTrue(driver.getPageSource().contains(regularUser.getUsername()));
        assertTrue(driver.getPageSource().contains(inactiveUser.getUsername()));
        assertTrue(driver.getPageSource().contains("Crear usuario"));
        assertTrue(driver.getPageSource().contains("Ver"));
        assertTrue(driver.getPageSource().contains("Editar"));
        assertTrue(driver.getPageSource().contains("Desactivar"));
        assertTrue(driver.getPageSource().contains("Activar"));
    }

    @Test
    void adminCanSeeUserDetail() {
        loginAdmin();

        driver.get(baseUrl + "admin/users/" + regularUser.getId());

        assertTrue(driver.getPageSource().contains(regularUser.getUsername()));
        assertTrue(driver.getPageSource().contains(regularUser.getEmail()));
        assertTrue(driver.getPageSource().contains("ROLE_USER"));
        assertTrue(driver.getPageSource().contains("Editar"));
        assertTrue(driver.getPageSource().contains("Desactivar"));
    }

    @Test
    void adminCanOpenCreateUserForm() {
        loginAdmin();

        driver.get(baseUrl + "admin/users/new");

        assertTrue(driver.getPageSource().contains("Crear usuario"));
        assertNotNull(driver.findElement(By.id("username")));
        assertNotNull(driver.findElement(By.id("email")));
        assertNotNull(driver.findElement(By.id("password")));
        assertNotNull(driver.findElement(By.id("role")));
        assertNotNull(driver.findElement(By.id("imageFile")));
    }

    @Test
    void regularUserCanSeeOwnProfile() {
        loginRegularUser();

        driver.get(baseUrl + "profile");

        wait.until(ExpectedConditions.urlContains("/profile"));

        assertTrue(driver.getPageSource().contains(regularUser.getUsername()));
        assertTrue(driver.getPageSource().contains(regularUser.getEmail()));
        assertTrue(driver.getPageSource().contains("Editar mi perfil"));

        assertFalse(driver.getCurrentUrl().contains("/admin/users"));
    }

    @Test
    void regularUserCanOpenEditProfileForm() {
        loginRegularUser();

        driver.get(baseUrl + "profile/edit");

        assertTrue(driver.getPageSource().contains("Editar mi perfil"));
        assertEquals(regularUser.getUsername(), driver.findElement(By.id("username")).getAttribute("value"));
        assertEquals(regularUser.getEmail(), driver.findElement(By.id("email")).getAttribute("value"));
        assertEquals("", driver.findElement(By.id("password")).getAttribute("value"));
        assertNotNull(driver.findElement(By.id("imageFile")));
    }

    @Test
    void adminCanDeactivateAndActivateRegularUser() {
        loginAdmin();

        driver.get(baseUrl + "admin/users");

        WebElement deactivateButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("form[action='/admin/users/deactivate/" + regularUser.getId() + "'] button[type='submit']")
        ));

        clickSafely(deactivateButton);

        wait.until(ExpectedConditions.textToBePresentInElementLocated(
                By.tagName("body"), "Usuario desactivado correctamente"));

        User deactivated = userRepository.findById(regularUser.getId()).orElseThrow();
        assertFalse(deactivated.getActive());

        WebElement activateButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("form[action='/admin/users/activate/" + regularUser.getId() + "'] button[type='submit']")
        ));

        clickSafely(activateButton);

        wait.until(ExpectedConditions.textToBePresentInElementLocated(
                By.tagName("body"), "Usuario activado correctamente"));

        User activated = userRepository.findById(regularUser.getId()).orElseThrow();
        assertTrue(activated.getActive());
    }

    private void loginAdmin() {
        login("admin_user_selenium", "admin123");
    }

    private void loginRegularUser() {
        login("regular_user_selenium", "user123");
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

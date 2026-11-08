package heritage.africca.accel_connect.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;


import static org.junit.jupiter.api.Assertions.*;


class MenuTest {

    private Menu menu;
    private Restaurant restaurant;

    @BeforeEach
    void setUp() {
        menu = new Menu();
        restaurant = new Restaurant();
        restaurant.setId(1L);
        restaurant.setRestaurantName("Restaurant Test");
    }

    @Test
    @DisplayName("Test du constructeur par défaut")
    void testConstructeurParDefaut() {
        assertNotNull(menu);
        assertNull(menu.getId());
        assertNull(menu.getRestaurant());
        assertNull(menu.getMenuDate());
        assertNotNull(menu.getMeals());
        assertTrue(menu.getMeals().isEmpty());
    }





    @Test
    @DisplayName("Test avec différentes dates")
    void testDifferentesDates() {
        LocalDate date1 = LocalDate.of(2024, 1, 1);
        menu.setMenuDate(date1);
        assertEquals(date1, menu.getMenuDate());

        LocalDate date2 = LocalDate.of(2024, 12, 31);
        menu.setMenuDate(date2);
        assertEquals(date2, menu.getMenuDate());
    }
}


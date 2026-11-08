package heritage.africca.accel_connect.service.Impl;

import heritage.africca.accel_connect.dto.MealDTO;
import heritage.africca.accel_connect.dto.MenuDTO;
import heritage.africca.accel_connect.entity.Meal;
import heritage.africca.accel_connect.entity.Menu;
import heritage.africca.accel_connect.entity.Restaurant;
import heritage.africca.accel_connect.repository.MealRepository;
import heritage.africca.accel_connect.repository.MenuRepository;
import heritage.africca.accel_connect.repository.RestaurantRepository;
import heritage.africca.accel_connect.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private final MenuRepository menuRepository;
    private final RestaurantRepository restaurantRepository;
    private final MealRepository mealRepository;

    @Transactional
    public MenuDTO createOrUpdateMenu(Long restaurantId, LocalDate menuDate, List<Long> mealIds) {
        if (menuDate.getDayOfWeek() == DayOfWeek.SATURDAY || menuDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Les menus ne peuvent pas être créés pour le week-end");
        }

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant non trouvé"));

        List<Meal> meals = mealRepository.findAllById(mealIds);
        if (meals.size() != mealIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Certains plats n'existent pas");
        }

        // Vérifier si un menu existe déjà pour cette date
        Menu menu = menuRepository.findByRestaurantAndMenuDate(restaurant, menuDate)
                .orElse(new Menu());

        menu.setRestaurant(restaurant);
        menu.setMenuDate(menuDate);
        menu.setMeals(meals);
        menu.setIsActive(true);

        Menu savedMenu = menuRepository.save(menu);
        return convertToDTO(savedMenu);
    }

    public MenuDTO getTodayMenu(Long restaurantId) {
        LocalDate today = LocalDate.now();

        if (today.getDayOfWeek() == DayOfWeek.SATURDAY || today.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Aucun menu disponible le week-end");
        }

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant non trouvé"));

        Menu menu = menuRepository.findByRestaurantAndMenuDate(restaurant, today)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucun menu défini pour aujourd'hui"));

        return convertToDTO(menu);
    }

    public MenuDTO getMenuByDate(Long restaurantId, LocalDate date) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant non trouvé"));

        Menu menu = menuRepository.findByRestaurantAndMenuDate(restaurant, date)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucun menu pour cette date"));

        return convertToDTO(menu);
    }

    public List<MenuDTO> getAllMenusByRestaurant(Long restaurantId) {
        return menuRepository.findByRestaurantId(restaurantId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<MenuDTO> getActiveMenus(LocalDate date) {
        return menuRepository.findByMenuDateAndIsActiveTrue(date).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteMenu(Long menuId) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu non trouvé"));

        menu.setIsActive(false);
        menuRepository.save(menu);
    }

    private MenuDTO convertToDTO(Menu menu) {
        MenuDTO dto = new MenuDTO();
        dto.setId(menu.getId());
        dto.setRestaurantId(menu.getRestaurant().getId());
        dto.setRestaurantName(menu.getRestaurant().getRestaurantName());
        dto.setMenuDate(menu.getMenuDate());
        dto.setIsActive(menu.getIsActive());

        List<MealDTO> mealDTOs = menu.getMeals().stream()
                .map(meal -> {
                    MealDTO mealDTO = new MealDTO();
                    mealDTO.setId(meal.getId());
                    mealDTO.setMealName(meal.getMealName());
                    mealDTO.setUnitPrice(meal.getUnitPrice());
                    return mealDTO;
                })
                .collect(Collectors.toList());

        dto.setMeals(mealDTOs);
        dto.setMealIds(menu.getMeals().stream().map(Meal::getId).collect(Collectors.toList()));

        return dto;
    }
}
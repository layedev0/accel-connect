package heritage.africca.accel_connect.service;

import heritage.africca.accel_connect.dto.MenuDTO;

import java.time.LocalDate;
import java.util.List;

public interface MenuService {
    MenuDTO createOrUpdateMenu(Long restaurantId, LocalDate menuDate, List<Long> mealIds);
    MenuDTO getTodayMenu(Long restaurantId);
    MenuDTO getMenuByDate(Long restaurantId, LocalDate date);
    List<MenuDTO> getAllMenusByRestaurant(Long restaurantId);
    List<MenuDTO> getActiveMenus(LocalDate date);
    void deleteMenu(Long menuId);
}

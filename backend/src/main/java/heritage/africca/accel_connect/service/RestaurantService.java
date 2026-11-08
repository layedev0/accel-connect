package heritage.africca.accel_connect.service;

import heritage.africca.accel_connect.dto.RestaurantDTO;

import java.util.List;

public interface RestaurantService {
    List<RestaurantDTO> getAllRestaurants();
    RestaurantDTO getRestaurantById(Long id);
    RestaurantDTO createRestaurant(RestaurantDTO dto);
    RestaurantDTO updateRestaurant(Long id, RestaurantDTO dto);
    RestaurantDTO addMealToRestaurant(Long restaurantId, Long mealId);
    RestaurantDTO removeMealFromRestaurant(Long restaurantId, Long mealId);
    void deleteRestaurant(Long id);

    RestaurantDTO toggleOrdersBlock(Long restaurantId, String blockMessage);
}
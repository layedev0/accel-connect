package heritage.africca.accel_connect.service.Impl;

import heritage.africca.accel_connect.dto.MealDTO;
import heritage.africca.accel_connect.dto.RestaurantDTO;
import heritage.africca.accel_connect.entity.Meal;
import heritage.africca.accel_connect.entity.Restaurant;
import heritage.africca.accel_connect.repository.MealRepository;
import heritage.africca.accel_connect.repository.RestaurantRepository;
import heritage.africca.accel_connect.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RestaurantServiceImpl implements RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final MealRepository mealRepository;

    @Transactional(readOnly = true)
    @Override
    public List<RestaurantDTO> getAllRestaurants() {
        return restaurantRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public RestaurantDTO getRestaurantById(Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Restaurant non trouvé: " + id));
        return convertToDTO(restaurant);
    }

    @Transactional
    @Override
    public RestaurantDTO createRestaurant(RestaurantDTO dto) {
        Restaurant restaurant = new Restaurant();
        restaurant.setRestaurantName(dto.getRestaurantName());
        restaurant.setAddress(dto.getAddress());
        restaurant.setContact(dto.getContact());
        restaurant.setDeliveryFee(dto.getDeliveryFee() != null ? dto.getDeliveryFee() : 500.0);
        restaurant.setOrdersBlocked(false);

        Restaurant saved = restaurantRepository.save(restaurant);
        log.info("✅ Restaurant créé: {}", saved.getRestaurantName());

        return convertToDTO(saved);
    }

    @Transactional
    @Override
    public RestaurantDTO updateRestaurant(Long id, RestaurantDTO dto) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Restaurant non trouvé: " + id));

        if (dto.getRestaurantName() != null) {
            restaurant.setRestaurantName(dto.getRestaurantName());
        }
        if (dto.getAddress() != null) {
            restaurant.setAddress(dto.getAddress());
        }
        if (dto.getContact() != null) {
            restaurant.setContact(dto.getContact());
        }
        if (dto.getDeliveryFee() != null) {
            restaurant.setDeliveryFee(dto.getDeliveryFee());
        }

        Restaurant updated = restaurantRepository.save(restaurant);
        log.info("✅ Restaurant mis à jour: {}", updated.getRestaurantName());

        return convertToDTO(updated);
    }

    @Transactional
    @Override
    public RestaurantDTO toggleOrdersBlock(Long restaurantId, String blockMessage) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Restaurant non trouvé: " + restaurantId));

        // Toggle le statut de blocage
        restaurant.setOrdersBlocked(!restaurant.getOrdersBlocked());

        // Si on bloque, on définit le message
        if (restaurant.getOrdersBlocked()) {
            if (blockMessage != null && !blockMessage.trim().isEmpty()) {
                restaurant.setBlockMessage(blockMessage.trim());
            } else {
                restaurant.setBlockMessage("Les commandes sont temporairement fermées pour ce restaurant.");
            }
            log.info("🔒 Restaurant {} : commandes BLOQUÉES - Message: {}",
                    restaurant.getRestaurantName(),
                    restaurant.getBlockMessage());
        } else {
            // Si on débloque, on efface le message
            restaurant.setBlockMessage(null);
            log.info("🔓 Restaurant {} : commandes DÉBLOQUÉES", restaurant.getRestaurantName());
        }

        Restaurant saved = restaurantRepository.save(restaurant);
        return convertToDTO(saved);
    }

    @Transactional
    @Override
    public RestaurantDTO addMealToRestaurant(Long restaurantId, Long mealId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Restaurant non trouvé: " + restaurantId));

        Meal meal = mealRepository.findById(mealId)
                .orElseThrow(() -> new RuntimeException("Plat non trouvé: " + mealId));

        if (!restaurant.getMeals().contains(meal)) {
            restaurant.getMeals().add(meal);
            restaurantRepository.save(restaurant);
            log.info("✅ Plat {} ajouté au restaurant {}", meal.getMealName(), restaurant.getRestaurantName());
        }

        return convertToDTO(restaurant);
    }

    @Transactional
    @Override
    public RestaurantDTO removeMealFromRestaurant(Long restaurantId, Long mealId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Restaurant non trouvé: " + restaurantId));

        restaurant.getMeals().removeIf(meal -> meal.getId().equals(mealId));
        restaurantRepository.save(restaurant);

        log.info("✅ Plat {} retiré du restaurant {}", mealId, restaurant.getRestaurantName());

        return convertToDTO(restaurant);
    }

    @Transactional
    @Override
    public void deleteRestaurant(Long id) {
        if (!restaurantRepository.existsById(id)) {
            throw new RuntimeException("Restaurant non trouvé: " + id);
        }
        restaurantRepository.deleteById(id);
        log.info("✅ Restaurant supprimé: {}", id);
    }

    private RestaurantDTO convertToDTO(Restaurant restaurant) {
        RestaurantDTO dto = new RestaurantDTO();
        dto.setId(restaurant.getId());
        dto.setRestaurantName(restaurant.getRestaurantName());
        dto.setAddress(restaurant.getAddress());
        dto.setContact(restaurant.getContact());
        dto.setDeliveryFee(restaurant.getDeliveryFee());
        dto.setOrdersBlocked(restaurant.getOrdersBlocked());
        dto.setBlockMessage(restaurant.getBlockMessage());

        if (restaurant.getMeals() != null && !restaurant.getMeals().isEmpty()) {
            List<MealDTO> mealDTOs = restaurant.getMeals().stream()
                    .map(meal -> {
                        MealDTO mealDTO = new MealDTO();
                        mealDTO.setId(meal.getId());
                        mealDTO.setMealName(meal.getMealName());
                        mealDTO.setUnitPrice(meal.getUnitPrice());
                        mealDTO.setImageUrl(meal.getImageUrl());
                        return mealDTO;
                    })
                    .collect(Collectors.toList());
            dto.setMeals(mealDTOs);
        }

        return dto;
    }
}
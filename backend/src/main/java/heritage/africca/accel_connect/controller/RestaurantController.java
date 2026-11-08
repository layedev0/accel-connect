package heritage.africca.accel_connect.controller;

import heritage.africca.accel_connect.dto.RestaurantDTO;
import heritage.africca.accel_connect.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/restaurants")
@CrossOrigin("*")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;

    /**
     * 📋 RÉCUPÈRE TOUS LES RESTAURANTS (Public)
     */
    @GetMapping("/all")
    public ResponseEntity<List<RestaurantDTO>> getAllRestaurants() {
        List<RestaurantDTO> restaurants = restaurantService.getAllRestaurants();
        return ResponseEntity.ok(restaurants);
    }

    /**
     * 🔍 RÉCUPÈRE UN RESTAURANT PAR ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<RestaurantDTO> getRestaurantById(@PathVariable Long id) {
        RestaurantDTO restaurant = restaurantService.getRestaurantById(id);
        return ResponseEntity.ok(restaurant);
    }

    /**
     * ➕ CRÉE UN RESTAURANT (ADMIN)
     */
    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<RestaurantDTO> createRestaurant(@RequestBody RestaurantDTO dto) {
        RestaurantDTO created = restaurantService.createRestaurant(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * ✏️ MET À JOUR UN RESTAURANT (ADMIN)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<RestaurantDTO> updateRestaurant(
            @PathVariable Long id,
            @RequestBody RestaurantDTO dto
    ) {
        RestaurantDTO updated = restaurantService.updateRestaurant(id, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * 🔒 BLOQUE/DÉBLOQUE LES COMMANDES D'UN RESTAURANT (ADMIN)
     */
    @PostMapping("/{restaurantId}/toggle-block")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<RestaurantDTO> toggleOrdersBlock(
            @PathVariable Long restaurantId,
            @RequestBody(required = false) Map<String, String> payload
    ) {
        String blockMessage = payload != null ? payload.get("blockMessage") : null;
        RestaurantDTO updated = restaurantService.toggleOrdersBlock(restaurantId, blockMessage);
        return ResponseEntity.ok(updated);
    }

    /**
     * 🍽️ ASSIGNE UN PLAT À UN RESTAURANT (ADMIN)
     */
    @PostMapping("/{restaurantId}/meals/{mealId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<RestaurantDTO> addMealToRestaurant(
            @PathVariable Long restaurantId,
            @PathVariable Long mealId
    ) {
        RestaurantDTO updated = restaurantService.addMealToRestaurant(restaurantId, mealId);
        return ResponseEntity.ok(updated);
    }

    /**
     * ❌ RETIRE UN PLAT D'UN RESTAURANT (ADMIN)
     */
    @DeleteMapping("/{restaurantId}/meals/{mealId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<RestaurantDTO> removeMealFromRestaurant(
            @PathVariable Long restaurantId,
            @PathVariable Long mealId
    ) {
        RestaurantDTO updated = restaurantService.removeMealFromRestaurant(restaurantId, mealId);
        return ResponseEntity.ok(updated);
    }

    /**
     * 🗑️ SUPPRIME UN RESTAURANT (ADMIN)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteRestaurant(@PathVariable Long id) {
        restaurantService.deleteRestaurant(id);
        return ResponseEntity.noContent().build();
    }
}
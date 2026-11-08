package heritage.africca.accel_connect.controller;

import heritage.africca.accel_connect.dto.MenuDTO;
import heritage.africca.accel_connect.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/menus")
@CrossOrigin("*")
public class MenuController {

    private final MenuService menuService;


    // Créer ou mettre à jour un menu pour un restaurant et une date
    @PostMapping
    public ResponseEntity<MenuDTO> createOrUpdateMenu(@RequestBody Map<String, Object> request) {
        Long restaurantId = Long.valueOf(request.get("restaurantId").toString());
        LocalDate menuDate = LocalDate.parse(request.get("menuDate").toString());

        @SuppressWarnings("unchecked")
        List<Long> mealIds = ((List<Integer>) request.get("mealIds")).stream()
                .map(Long::valueOf)
                .toList();

        MenuDTO menu = menuService.createOrUpdateMenu(restaurantId, menuDate, mealIds);
        return ResponseEntity.status(HttpStatus.CREATED).body(menu);
    }

    // Récupérer le menu du jour pour un restaurant
    @GetMapping("/today/{restaurantId}")
    public ResponseEntity<MenuDTO> getTodayMenu(@PathVariable Long restaurantId) {
        MenuDTO menu = menuService.getTodayMenu(restaurantId);
        return ResponseEntity.ok(menu);
    }

    // Récupérer un menu pour une date spécifique
    @GetMapping("/{restaurantId}")
    public ResponseEntity<MenuDTO> getMenuByDate(
            @PathVariable Long restaurantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        MenuDTO menu = menuService.getMenuByDate(restaurantId, date);
        return ResponseEntity.ok(menu);
    }

    // Récupérer tous les menus d'un restaurant
    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<List<MenuDTO>> getAllMenusByRestaurant(@PathVariable Long restaurantId) {
        List<MenuDTO> menus = menuService.getAllMenusByRestaurant(restaurantId);
        return ResponseEntity.ok(menus);
    }

    // Récupérer tous les menus actifs pour une date
    @GetMapping("/active")
    public ResponseEntity<List<MenuDTO>> getActiveMenus(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<MenuDTO> menus = menuService.getActiveMenus(date);
        return ResponseEntity.ok(menus);
    }

    // Désactiver un menu
    @DeleteMapping("/{menuId}")
    public ResponseEntity<Void> deleteMenu(@PathVariable Long menuId) {
        menuService.deleteMenu(menuId);
        return ResponseEntity.noContent().build();
    }
}
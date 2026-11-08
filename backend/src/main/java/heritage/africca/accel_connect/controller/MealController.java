package heritage.africca.accel_connect.controller;

import heritage.africca.accel_connect.dto.MealDTO;
import heritage.africca.accel_connect.service.MealService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/meals")
@CrossOrigin("*")
@RequiredArgsConstructor
public class MealController {

    private final MealService mealService;

    /**
     * 📋 RÉCUPÈRE TOUS LES PLATS (Public)
     */
    @GetMapping("/all")
    public ResponseEntity<List<MealDTO>> getAllMeals() {
        List<MealDTO> meals = mealService.getAllMeals();
        return ResponseEntity.ok(meals);
    }

    /**
     * 🔍 RÉCUPÈRE UN PLAT PAR ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<MealDTO> getMealById(@PathVariable Long id) {
        MealDTO meal = mealService.getMealById(id);
        return ResponseEntity.ok(meal);
    }

    /**
     * ➕ CRÉE UN PLAT (ADMIN)
     */
    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<MealDTO> createMeal(@RequestBody MealDTO dto) {
        MealDTO created = mealService.createMeal(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * ✏️ MET À JOUR UN PLAT (ADMIN)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<MealDTO> updateMeal(
            @PathVariable Long id,
            @RequestBody MealDTO dto
    ) {
        MealDTO updated = mealService.updateMeal(id, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * 🗑️ SUPPRIME UN PLAT (ADMIN)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteMeal(@PathVariable Long id) {
        mealService.deleteMeal(id);
        return ResponseEntity.noContent().build();
    }
}
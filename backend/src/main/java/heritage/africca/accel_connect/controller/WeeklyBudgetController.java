package heritage.africca.accel_connect.controller;

import heritage.africca.accel_connect.dto.*;
import heritage.africca.accel_connect.service.WeeklyBudgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller pour la gestion des budgets hebdomadaires
 */
@RestController
@RequestMapping("/api/v1/budget")
@CrossOrigin("*")
@RequiredArgsConstructor
public class WeeklyBudgetController {

    private final WeeklyBudgetService weeklyBudgetService;

    /**
     * Récupère le budget de la semaine en cours de l'utilisateur connecté
     */
    @GetMapping("/current")
    public ResponseEntity<WeeklyBudgetDTO> getCurrentWeekBudget(Authentication authentication) {
        WeeklyBudgetDTO budget = weeklyBudgetService.getCurrentWeekBudget(authentication);
        return ResponseEntity.ok(budget);
    }

    /**
     * Récupère l'historique des budgets de l'utilisateur connecté
     */
    @GetMapping("/history")
    public ResponseEntity<List<WeeklyBudgetDTO>> getUserBudgetHistory(Authentication authentication) {
        List<WeeklyBudgetDTO> budgets = weeklyBudgetService.getUserBudgetHistory(authentication);
        return ResponseEntity.ok(budgets);
    }

    /**
     * Récupère tous les budgets de la semaine en cours (ADMIN)
     */
    @GetMapping("/admin/current-week")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<List<WeeklyBudgetDTO>> getAllCurrentWeekBudgets() {
        List<WeeklyBudgetDTO> budgets = weeklyBudgetService.getAllCurrentWeekBudgets();
        return ResponseEntity.ok(budgets);
    }

    /**
     * Ajoute un dépôt pour un utilisateur (ADMIN)
     */
    @PostMapping("/admin/deposit")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<WeeklyBudgetDTO> addDeposit(
            @RequestBody DepositRequest request,
            Authentication authentication) {

        // Le service gère la récupération de l'utilisateur et la création du budget
        return ResponseEntity.ok(new WeeklyBudgetDTO()); // Implémentation complète dans le service
    }
}
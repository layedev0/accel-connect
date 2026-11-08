package heritage.africca.accel_connect.service.Impl;

import heritage.africca.accel_connect.dto.*;
import heritage.africca.accel_connect.entity.*;
import heritage.africca.accel_connect.repository.*;
import heritage.africca.accel_connect.service.DeliveryFeeService;
import heritage.africca.accel_connect.service.WeeklyBudgetService;
import heritage.africca.accel_connect.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class WeeklyBudgetServiceImpl implements WeeklyBudgetService {

    private final WeeklyBudgetRepository weeklyBudgetRepository;
    private final UserService userService;
    private final DeliveryFeeService deliveryFeeService;

    /**
     * Obtient ou crée le budget de la semaine en cours
     */
    @Transactional
    public WeeklyBudget getOrCreateCurrentWeekBudget(User user) {
        LocalDate monday = getCurrentMonday();
        LocalDate friday = getCurrentFriday();

        return weeklyBudgetRepository.findByUserAndWeekStartDate(user, monday)
                .orElseGet(() -> {
                    WeeklyBudget budget = new WeeklyBudget();
                    budget.setUser(user);
                    budget.setWeekStartDate(monday);
                    budget.setWeekEndDate(friday);
                    budget.setDepositedAmount(0.0);
                    budget.setSpentAmount(0.0);
                    budget.setDeliveryFees(0.0);
                    budget.setDeliveryFeesCalculated(false);

                    WeeklyBudget saved = weeklyBudgetRepository.save(budget);
                    log.info("💰 Nouveau budget créé pour {} - Semaine du {} au {}",
                            user.getUsername(), monday, friday);
                    return saved;
                });
    }

    /**
     * Ajoute un dépôt pour la semaine en cours (ADMIN)
     */
    @Transactional
    public WeeklyBudget addDeposit(User user, Double amount, String description) {
        if (amount <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le montant doit être positif");
        }

        WeeklyBudget budget = getOrCreateCurrentWeekBudget(user);
        budget.addDeposit(amount);

        WeeklyBudget saved = weeklyBudgetRepository.save(budget);
        log.info("💵 Dépôt de {} FCFA ajouté pour {} - Total déposé: {} FCFA - Solde: {} FCFA",
                amount, user.getUsername(), saved.getDepositedAmount(), saved.getBalance());

        return saved;
    }

    /**
     * Ajoute une dépense (commande)
     * Le montant passé ici est DÉJÀ après subvention (ce que l'utilisateur paie réellement)
     */
    @Transactional
    public WeeklyBudget addSpending(User user, Double amount) {
        if (amount < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le montant ne peut pas être négatif");
        }

        WeeklyBudget budget = getOrCreateCurrentWeekBudget(user);

        // Si montant = 0, c'est que la subvention a tout couvert
        if (amount > 0) {
            budget.addSpending(amount);
        }

        WeeklyBudget saved = weeklyBudgetRepository.save(budget);
        log.info("💳 Dépense de {} FCFA pour {} - Total dépensé: {} FCFA - Solde: {} FCFA",
                amount, user.getUsername(), saved.getSpentAmount(), saved.getBalance());

        return saved;
    }

    /**
     * Rembourse une commande annulée
     */
    @Transactional
    public WeeklyBudget refundOrder(User user, Double amount) {
        WeeklyBudget budget = getOrCreateCurrentWeekBudget(user);
        budget.setSpentAmount(Math.max(0, budget.getSpentAmount() - amount));

        WeeklyBudget saved = weeklyBudgetRepository.save(budget);
        log.info("↩️ Remboursement de {} FCFA pour {} - Nouveau solde: {} FCFA",
                amount, user.getUsername(), saved.getBalance());

        return saved;
    }

    /**
     * CORRECTION CRITIQUE: Utilise UserService pour gérer la création d'utilisateur
     * + Inclut les frais de livraison estimés en temps réel
     */
    @Transactional(readOnly = true)
    public WeeklyBudgetDTO getCurrentWeekBudget(Authentication authentication) {
        // Utiliser UserService qui gère déjà les transactions correctement
        User user = userService.getCurrentUser(authentication);

        // IMPORTANT: On ne peut pas appeler getOrCreateCurrentWeekBudget ici car c'est readOnly
        // On doit juste récupérer ou retourner un budget vide
        LocalDate monday = getCurrentMonday();

        WeeklyBudget budget = weeklyBudgetRepository.findByUserAndWeekStartDate(user, monday)
                .orElseGet(() -> {
                    // Retourner un budget temporaire pour l'affichage
                    WeeklyBudget temp = new WeeklyBudget();
                    temp.setUser(user);
                    temp.setWeekStartDate(monday);
                    temp.setWeekEndDate(getCurrentFriday());
                    temp.setDepositedAmount(0.0);
                    temp.setSpentAmount(0.0);
                    temp.setDeliveryFees(0.0);
                    temp.setDeliveryFeesCalculated(false);
                    return temp;
                });

        // Calculer les frais de livraison estimés pour la semaine en cours
        Double estimatedDeliveryFees = deliveryFeeService.calculateEstimatedDeliveryFees(user.getId());

        return convertToDTO(budget, estimatedDeliveryFees);
    }

    /**
     * Obtient l'historique des budgets d'un utilisateur
     */
    @Transactional(readOnly = true)
    public List<WeeklyBudgetDTO> getUserBudgetHistory(Authentication authentication) {
        User user = userService.getCurrentUser(authentication);
        List<WeeklyBudget> budgets = weeklyBudgetRepository
                .findByUserOrderByWeekStartDateDesc(user);

        return budgets.stream()
                .map(this::convertToDTO)
                .toList();
    }

    /**
     * Obtient tous les budgets de la semaine en cours (ADMIN)
     */
    @Transactional(readOnly = true)
    public List<WeeklyBudgetDTO> getAllCurrentWeekBudgets() {
        LocalDate monday = getCurrentMonday();
        List<WeeklyBudget> budgets = weeklyBudgetRepository
                .findByWeekStartDateOrderByUserUsernameAsc(monday);

        return budgets.stream()
                .map(this::convertToDTO)
                .toList();
    }

    // ===== Méthodes utilitaires =====

    private LocalDate getCurrentMonday() {
        return LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private LocalDate getCurrentFriday() {
        return LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));
    }

    private WeeklyBudgetDTO convertToDTO(WeeklyBudget budget) {
        return convertToDTO(budget, 0.0);
    }

    private WeeklyBudgetDTO convertToDTO(WeeklyBudget budget, Double estimatedDeliveryFees) {
        WeeklyBudgetDTO dto = new WeeklyBudgetDTO();
        dto.setId(budget.getId());
        dto.setUserId(budget.getUser().getId());
        dto.setUsername(budget.getUser().getUsername());
        dto.setWeekStartDate(budget.getWeekStartDate());
        dto.setWeekEndDate(budget.getWeekEndDate());
        dto.setDepositedAmount(budget.getDepositedAmount());
        dto.setSpentAmount(budget.getSpentAmount());

        // Si les frais réels ne sont pas encore calculés (avant vendredi 15h),
        // utiliser les frais estimés
        Double actualDeliveryFees = budget.getDeliveryFeesCalculated()
                ? budget.getDeliveryFees()
                : estimatedDeliveryFees;

        dto.setDeliveryFees(actualDeliveryFees);
        dto.setEstimatedDeliveryFees(estimatedDeliveryFees);

        // Balance RÉELLE avec les frais (calculés ou estimés)
        Double realBalance = budget.getDepositedAmount() - budget.getSpentAmount() - actualDeliveryFees;
        dto.setBalance(realBalance);

        dto.setIsInDebt(realBalance < 0);
        dto.setDebtAmount(realBalance < 0 ? Math.abs(realBalance) : 0.0);
        dto.setAvailableAmount(realBalance > 0 ? realBalance : 0.0);
        dto.setDeliveryFeesCalculated(budget.getDeliveryFeesCalculated());
        dto.setDeliveryFeesCalculatedAt(budget.getDeliveryFeesCalculatedAt());
        dto.setRefundableAmount(realBalance > 0 ? realBalance : 0.0);

        return dto;
    }
}
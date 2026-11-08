package heritage.africca.accel_connect.service;

import heritage.africca.accel_connect.dto.WeeklyBudgetDTO;
import heritage.africca.accel_connect.entity.*;
import heritage.africca.accel_connect.repository.*;
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

public interface WeeklyBudgetService {

    /**
     * Obtient ou crée le budget de la semaine en cours
     */
    WeeklyBudget getOrCreateCurrentWeekBudget(User user);

    /**
     * Ajoute un dépôt au budget de la semaine (ADMIN)
     */
    WeeklyBudget addDeposit(User user, Double amount, String description);

    /**
     * Ajoute une dépense (commande)
     */
    WeeklyBudget addSpending(User user, Double amount);

    /**
     * Rembourse une commande annulée
     */
    WeeklyBudget refundOrder(User user, Double amount);

    /**
     * Obtient le budget de la semaine en cours
     */
    WeeklyBudgetDTO getCurrentWeekBudget(Authentication authentication);

    /**
     * Obtient l'historique des budgets d'un utilisateur
     */
    List<WeeklyBudgetDTO> getUserBudgetHistory(Authentication authentication);

    /**
     * Obtient tous les budgets de la semaine en cours (ADMIN)
     */
    List<WeeklyBudgetDTO> getAllCurrentWeekBudgets();
}
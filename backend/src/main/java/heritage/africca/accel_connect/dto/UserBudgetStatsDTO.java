package heritage.africca.accel_connect.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour les statistiques d'affichage (restauration component)
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserBudgetStatsDTO {
    private Double weeklyDeposit;      // Montant donné cette semaine
    private Double weeklySpent;        // Montant dépensé cette semaine
    private Double remainingBalance;   // Ce qui reste (ou dette)
    private Boolean isInDebt;
    private String statusMessage;
}


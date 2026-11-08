package heritage.africca.accel_connect.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO pour le budget hebdomadaire
 * LOGIQUE: Montant déposé cette semaine - Montant dépensé = Solde à rembourser
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyBudgetDTO {

    private Long id;
    private Long userId;
    private String username;
    private LocalDate weekStartDate;
    private LocalDate weekEndDate;

    /**
     * Montant déposé par l'utilisateur CETTE semaine
     */
    private Double depositedAmount;

    /**
     * Montant dépensé CETTE semaine (commandes uniquement, hors subvention)
     */
    private Double spentAmount;

    /**
     * Frais de livraison calculés pour cette semaine
     */
    private Double deliveryFees;

    /**
     * Solde = depositedAmount - (spentAmount + deliveryFees)
     * Positif = À rembourser | Négatif = Dette
     */
    private Double balance;

    /**
     * L'utilisateur est-il en dette ?
     */
    private Boolean isInDebt;

    /**
     * Montant de la dette (si isInDebt = true)
     */
    private Double debtAmount;

    /**
     * Montant disponible (uniquement si positif)
     */
    private Double availableAmount;

    /**
     * Montant à rembourser en fin de semaine
     * C'est le solde positif restant
     */
    private Double refundableAmount;

    /**
     * Les frais de livraison ont-ils été calculés ?
     */
    private Boolean deliveryFeesCalculated;

    /**
     * Date de calcul des frais de livraison
     */
    private LocalDateTime deliveryFeesCalculatedAt;

    /**
     * Frais de livraison estimés pour la semaine en cours
     * (utilisé si deliveryFeesCalculated = false)
     */
    private Double estimatedDeliveryFees;
}
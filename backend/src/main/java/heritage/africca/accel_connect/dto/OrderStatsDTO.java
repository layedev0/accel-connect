package heritage.africca.accel_connect.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour les statistiques de commandes
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatsDTO {

    /**
     * Nombre total de commandes
     */
    private Integer totalOrders;

    /**
     * Montant total dépensé (hors subvention)
     */
    private Double totalSpent;

    /**
     * Nombre de commandes confirmées
     */
    private Integer confirmedCount;

    /**
     * Nombre de commandes annulées
     */
    private Integer cancelledCount;

    /**
     * Montant total des subventions utilisées
     */
    private Double totalSubsidyUsed;

    /**
     * Montant total des frais de livraison
     */
    private Double totalDeliveryFees;

    /**
     * Mois (1-12)
     */
    private Integer month;

    /**
     * Année
     */
    private Integer year;
}
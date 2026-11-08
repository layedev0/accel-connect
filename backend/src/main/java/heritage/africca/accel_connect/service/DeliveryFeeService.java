package heritage.africca.accel_connect.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;

public interface DeliveryFeeService {
    @Scheduled(cron = "0 0 15 * * FRI", zone = "Africa/Dakar")
    @Transactional
    void calculateWeeklyDeliveryFees();

    @Transactional
    void triggerManualCalculation();

    /**
     * 🔄 RECALCULE LES FRAIS DE LIVRAISON EN TEMPS RÉEL
     * Appelé automatiquement quand une commande est confirmée
     * Met à jour delivery_fee_share pour toutes les commandes du jour dans chaque restaurant
     *
     * @param orderDate La date de la commande
     */
    @Transactional
    void recalculateDeliveryFeesForDate(LocalDate orderDate);

    /**
     * Calcule les frais de livraison pour un utilisateur (lecture seule)
     * Utilise delivery_fee_share si disponible, sinon calcule en temps réel
     */
    @Transactional(readOnly = true)
    Double calculateEstimatedDeliveryFees(Long userId);
}

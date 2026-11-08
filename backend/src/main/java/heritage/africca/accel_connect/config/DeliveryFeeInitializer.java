package heritage.africca.accel_connect.config;

import heritage.africca.accel_connect.service.DeliveryFeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * 🚀 INITIALISATION AU DÉMARRAGE
 * Recalcule automatiquement les frais de livraison pour toute la semaine en cours
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryFeeInitializer {

    private final DeliveryFeeService deliveryFeeService;

    @EventListener(ApplicationReadyEvent.class)
    public void recalculateCurrentWeekFeesOnStartup() {
        try {
            log.info("🚀 === INITIALISATION: Recalcul des frais de livraison de la semaine ===");

            LocalDate today = LocalDate.now();
            LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate friday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));

            log.info("📅 Période: {} → {}", monday, friday);

            // Recalculer jour par jour
            LocalDate currentDate = monday;
            int daysProcessed = 0;

            while (!currentDate.isAfter(today)) {  // Jusqu'à aujourd'hui inclus
                log.info("🔄 Recalcul des frais pour le {}", currentDate);
                deliveryFeeService.recalculateDeliveryFeesForDate(currentDate);
                currentDate = currentDate.plusDays(1);
                daysProcessed++;
            }

            log.info("✅ === INITIALISATION TERMINÉE: {} jour(s) traité(s) ===", daysProcessed);

        } catch (Exception e) {
            log.error("❌ Erreur lors de l'initialisation des frais de livraison", e);
            // Ne pas bloquer le démarrage de l'application
        }
    }
}

package heritage.africca.accel_connect.repository;

import heritage.africca.accel_connect.entity.DeliveryFeeCalculation;
import heritage.africca.accel_connect.entity.Restaurant;
import heritage.africca.accel_connect.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DeliveryFeeCalculationRepository extends JpaRepository<DeliveryFeeCalculation, Long> {

    /**
     * Récupère les calculs d'un utilisateur pour une semaine
     */
    List<DeliveryFeeCalculation> findByUserAndWeekStartDate(User user, LocalDate weekStartDate);

    /**
     * Récupère les calculs d'un restaurant pour une semaine
     */
    List<DeliveryFeeCalculation> findByRestaurantAndWeekStartDate(Restaurant restaurant, LocalDate weekStartDate);

    /**
     * Récupère tous les calculs d'une semaine
     */
    List<DeliveryFeeCalculation> findByWeekStartDateOrderByRestaurantRestaurantName(LocalDate weekStartDate);
}

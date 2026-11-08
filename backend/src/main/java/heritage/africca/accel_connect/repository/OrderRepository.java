package heritage.africca.accel_connect.repository;

import heritage.africca.accel_connect.entity.Order;
import heritage.africca.accel_connect.entity.User;
import heritage.africca.accel_connect.entity.enumeration.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Récupère les commandes d'un utilisateur
     */
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Vérifie si l'utilisateur a déjà commandé aujourd'hui
     */
    boolean existsByUserAndOrderDate(User user, LocalDate orderDate);

    /**
     * Récupère la commande d'un utilisateur pour une date donnée
     */
    Optional<Order> findByUserAndOrderDate(User user, LocalDate orderDate);

    /**
     * Récupère les commandes d'une période avec un statut
     */
    List<Order> findByOrderDateBetweenAndStatus(
            LocalDate startDate, LocalDate endDate, OrderStatus status);

    /**
     * Récupère les commandes d'une date
     */
    List<Order> findByOrderDate(LocalDate orderDate);

    /**
     * Récupère les commandes par restaurant (via les items)
     */
    @Query("SELECT DISTINCT o FROM Order o JOIN o.orderItems oi WHERE oi.restaurant.id = :restaurantId")
    List<Order> findByRestaurantId(@Param("restaurantId") Long restaurantId);

    /**
     * Récupère les commandes confirmées d'un restaurant pour une date donnée
     */
    @Query("SELECT DISTINCT o FROM Order o JOIN o.orderItems oi WHERE oi.restaurant.id = :restaurantId AND o.orderDate = :date AND o.status = 'CONFIRMED'")
    List<Order> findConfirmedByRestaurantIdAndDate(@Param("restaurantId") Long restaurantId, @Param("date") LocalDate date);

    /**
     * Compte le nombre de commandes d'un utilisateur
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.user.id = :userId")
    Long countByUserId(@Param("userId") Long userId);

    List<Order> findByStatus(OrderStatus orderStatus);

    /**
     * Récupère les commandes d'un utilisateur pour une période avec un statut
     */
    List<Order> findByUserIdAndOrderDateBetweenAndStatus(
            Long userId, LocalDate startDate, LocalDate endDate, OrderStatus status);

    /**
     * Dépenses hebdomadaires d'un utilisateur (5 dernières semaines)
     */
    @Query(value = """
            SELECT DATE_TRUNC('week', o.order_date)::date AS week_start,
                   SUM(o.total_amount) AS total_spent
            FROM orders o
            WHERE o.user_id = :userId AND o.status = 'CONFIRMED'
            GROUP BY DATE_TRUNC('week', o.order_date)
            ORDER BY week_start DESC
            LIMIT 5
            """, nativeQuery = true)
    List<Object[]> findWeeklySpendingByUserId(@Param("userId") Long userId);
}
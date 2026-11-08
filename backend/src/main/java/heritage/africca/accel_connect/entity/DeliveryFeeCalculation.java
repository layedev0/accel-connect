package heritage.africca.accel_connect.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 🚚 CALCUL DES FRAIS DE LIVRAISON
 *
 * Trace pour chaque utilisateur et restaurant:
 * - Le nombre de jours où il a commandé dans la semaine
 * - Sa part des frais de livraison hebdomadaires
 *
 * Formule:
 * Part utilisateur = (frais_par_jour × nb_jours_utilisateur) ÷ nb_utilisateurs_uniques
 */
@Entity
@Table(name = "delivery_fee_calculations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryFeeCalculation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    /**
     * 📅 Lundi de la semaine concernée
     */
    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;

    /**
     * 📊 Nombre de JOURS où l'utilisateur a commandé dans ce restaurant
     */
    @Column(name = "order_days_count", nullable = false)
    private Integer orderDaysCount;

    /**
     * 👥 Nombre total d'utilisateurs UNIQUES ayant commandé dans ce restaurant cette semaine
     */
    @Column(name = "total_users", nullable = false)
    private Integer totalUsers;

    /**
     * 💰 Frais de livraison par jour du restaurant
     */
    @Column(name = "restaurant_daily_fee", nullable = false)
    private Double restaurantDailyFee;

    /**
     * 💵 Part de l'utilisateur
     * = (frais_par_jour × nb_jours_utilisateur) ÷ nb_utilisateurs
     */
    @Column(name = "user_share", nullable = false)
    private Double userShare;

    @Column(name = "calculated_at")
    private LocalDateTime calculatedAt;

    @PrePersist
    protected void onCreate() {
        this.calculatedAt = LocalDateTime.now();
    }
}
package heritage.africca.accel_connect.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 💳 ENREGISTREMENT DES PAIEMENTS AUX RESTAURANTS
 *
 * Trace chaque paiement effectué aux restaurants
 */
@Entity
@Table(name = "payment_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 📅 Date de début de la période
     */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /**
     * 📅 Date de fin de la période
     */
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /**
     * 💰 Montant total avec subvention
     */
    @Column(name = "total_amount_with_subsidy", nullable = false)
    private Double totalAmountWithSubsidy;

    /**
     * 💵 Montant total sans subvention (réel à payer)
     */
    @Column(name = "total_amount_without_subsidy", nullable = false)
    private Double totalAmountWithoutSubsidy;

    /**
     * 🎁 Subvention totale
     */
    @Column(name = "total_subsidy", nullable = false)
    private Double totalSubsidy;

    /**
     * 🚚 Frais de livraison totaux
     */
    @Column(name = "total_delivery_fees", nullable = false)
    private Double totalDeliveryFees;

    /**
     * 📦 Nombre de commandes
     */
    @Column(name = "order_count", nullable = false)
    private Integer orderCount;

    /**
     * 👥 Nombre d'utilisateurs
     */
    @Column(name = "user_count", nullable = false)
    private Integer userCount;

    /**
     * 📝 Détails JSON des restaurants
     */
    @Column(name = "restaurant_details", columnDefinition = "TEXT")
    private String restaurantDetails;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
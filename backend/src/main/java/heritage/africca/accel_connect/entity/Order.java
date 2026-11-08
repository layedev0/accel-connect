package heritage.africca.accel_connect.entity;

import heritage.africca.accel_connect.entity.enumeration.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 📦 COMMANDE - UNE SEULE PAR JOUR PAR UTILISATEUR
 *
 * Règles:
 * - Un utilisateur peut commander UNE SEULE FOIS par jour
 * - La commande peut contenir des plats de PLUSIEURS restaurants
 * - Subvention de 1000 FCFA appliquée AUTOMATIQUEMENT
 * - Frais de livraison calculés le vendredi à 15h
 * - Répartis équitablement entre tous les utilisateurs ayant commandé dans le même restaurant
 */
@Entity
@Table(
        name = "orders",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"user_id", "order_date"},
                name = "uk_user_order_date"
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 📅 Date de la commande (unique par utilisateur)
     */
    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    /**
     * 🍽️ Items de la commande (peut contenir des plats de plusieurs restaurants)
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    /**
     * 💰 SOUS-TOTAL (somme des prix des plats AVANT subvention)
     */
    @Column(name = "subtotal", nullable = false)
    private Double subtotal;

    /**
     * 🎁 SUBVENTION appliquée (toujours 1000 FCFA)
     */
    @Column(name = "subsidy_amount", nullable = false)
    private Double subsidyAmount = 1000.0;

    /**
     * 💵 MONTANT APRÈS SUBVENTION
     * = max(0, subtotal - 1000)
     * C'est ce que l'utilisateur paie IMMÉDIATEMENT
     */
    @Column(name = "amount_after_subsidy", nullable = false)
    private Double amountAfterSubsidy;

    /**
     * 🚚 FRAIS DE LIVRAISON (calculés vendredi à 15h)
     * Somme des parts de l'utilisateur pour chaque restaurant
     */
    @Column(name = "delivery_fee_share", nullable = false)
    private Double deliveryFeeShare = 0.0;

    /**
     * 💳 MONTANT TOTAL FINAL
     * = amountAfterSubsidy + deliveryFeeShare
     */
    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.CONFIRMED;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        // Calcul automatique
        this.amountAfterSubsidy = Math.max(0, this.subtotal - this.subsidyAmount);
        this.totalAmount = this.amountAfterSubsidy;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 🔄 Ajoute les frais de livraison (appelé vendredi à 15h)
     */
    public void addDeliveryFees(Double deliveryFee) {
        this.deliveryFeeShare += deliveryFee;
        this.totalAmount = this.amountAfterSubsidy + this.deliveryFeeShare;
    }

    /**
     * ➕ Ajoute un item à la commande
     */
    public void addOrderItem(OrderItem item) {
        if (orderItems == null) {
            orderItems = new ArrayList<>();
        }
        item.setOrder(this);
        item.calculateSubtotal(); // <-- calcule le sous-total avant d’ajouter
        orderItems.add(item);
    }

    /**
     * 📊 Calcule le sous-total
     */
    public void calculateSubtotal() {
        if (orderItems == null || orderItems.isEmpty()) {
            this.subtotal = 0.0;
            return;
        }

        this.subtotal = orderItems.stream()
                .filter(Objects::nonNull)
                .map(OrderItem::getSubtotal)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum();
    }

}
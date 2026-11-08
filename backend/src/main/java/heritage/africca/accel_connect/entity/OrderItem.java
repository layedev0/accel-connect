package heritage.africca.accel_connect.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 🍽️ ITEM DE COMMANDE
 *
 * Représente un plat dans une commande
 * Contient la référence au restaurant pour faciliter le calcul des frais de livraison
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_id", nullable = false)
    private Meal meal;

    /**
     * 🏪 Restaurant du plat (pour calculer les frais de livraison)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false)
    private Double unitPrice;

    @Column(name = "subtotal", nullable = false)
    private Double subtotal;

    @PrePersist
    @PreUpdate
    public void calculateSubtotal() {
        this.subtotal = this.unitPrice * this.quantity;
    }
}
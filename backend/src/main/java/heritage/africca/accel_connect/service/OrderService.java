package heritage.africca.accel_connect.service;

import heritage.africca.accel_connect.dto.AdminCreateOrderRequest;
import heritage.africca.accel_connect.dto.OrderCreationPayload;
import heritage.africca.accel_connect.dto.OrderDTO;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.List;

public interface OrderService {

    // ========== MÉTHODES UTILISATEUR ==========

    OrderDTO createOrder(Authentication auth, OrderCreationPayload payload);

    List<OrderDTO> getMyOrders(Authentication auth);

    OrderDTO cancelOrder(Authentication auth, Long orderId);

    List<OrderDTO> getOrdersByDate(LocalDate date);

    List<OrderDTO> getOrdersByRestaurant(Long restaurantId);

    // ========== MÉTHODES ADMIN ==========

    /**
     * ❌ Annule une commande (ADMIN - sans contrainte horaire)
     * Rembourse automatiquement l'utilisateur
     */
    OrderDTO adminCancelOrder(Long orderId, String reason);

    /**
     * 🗑️ Supprime un plat d'une commande
     * Recalcule automatiquement les montants et ajuste le UserBalance
     */
    OrderDTO adminRemoveOrderItem(Long orderId, Long orderItemId, String reason);

    /**
     * ✏️ Modifie la quantité d'un plat dans une commande
     * Recalcule automatiquement les montants et ajuste le UserBalance
     */
    OrderDTO adminUpdateOrderItemQuantity(Long orderId, Long orderItemId, Integer newQuantity, String reason);

    /**
     * 🔄 Réactive une commande annulée
     * Débite à nouveau l'utilisateur
     */
    OrderDTO adminReactivateOrder(Long orderId, String reason);

    /**
     * ➕ ADMIN: Crée ou remplace une commande pour un utilisateur (sans contrainte horaire)
     * Si une commande existe déjà pour cet utilisateur+date : elle est remplacée
     * Si elle était annulée : elle est réouverte
     */
    OrderDTO adminCreateOrder(AdminCreateOrderRequest request);
}
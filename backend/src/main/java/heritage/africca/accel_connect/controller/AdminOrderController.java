package heritage.africca.accel_connect.controller;

import heritage.africca.accel_connect.dto.AdminCreateOrderRequest;
import heritage.africca.accel_connect.dto.OrderDTO;
import heritage.africca.accel_connect.dto.OrderItemUpdateRequest;
import heritage.africca.accel_connect.service.DeliveryFeeService;
import heritage.africca.accel_connect.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 🛡️ CONTRÔLEUR ADMIN POUR LA GESTION DES COMMANDES
 */
@RestController
@RequestMapping("/api/v1/admin/orders")
@CrossOrigin("*")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')")
@Slf4j
public class AdminOrderController {

    private final OrderService orderService;
    private final DeliveryFeeService deliveryFeeService;

    /**
     * 📅 RÉCUPÈRE LES COMMANDES PAR DATE
     */
    @GetMapping("/by-date")
    public ResponseEntity<List<OrderDTO>> getOrdersByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<OrderDTO> orders = orderService.getOrdersByDate(date);
        return ResponseEntity.ok(orders);
    }

    /**
     * 🏪 RÉCUPÈRE LES COMMANDES PAR RESTAURANT
     */
    @GetMapping("/by-restaurant/{restaurantId}")
    public ResponseEntity<List<OrderDTO>> getOrdersByRestaurant(
            @PathVariable Long restaurantId
    ) {
        List<OrderDTO> orders = orderService.getOrdersByRestaurant(restaurantId);
        return ResponseEntity.ok(orders);
    }

    /**
     * ❌ ANNULE UNE COMMANDE (ADMIN - sans contrainte horaire)
     */
    @PutMapping("/{orderId}/admin-cancel")
    public ResponseEntity<?> adminCancelOrder(
            @PathVariable Long orderId,
            @RequestParam(required = false) String reason
    ) {
        try {
            OrderDTO order = orderService.adminCancelOrder(orderId, reason);
            log.info("✅ Admin a annulé la commande #{} - Raison: {}", orderId, reason);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Commande annulée avec succès");
            response.put("order", order);
            response.put("refundedAmount", order.getTotalAmount());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("❌ Erreur annulation commande #{}: {}", orderId, e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 🗑️ SUPPRIME UN PLAT D'UNE COMMANDE
     */
    @DeleteMapping("/{orderId}/items/{orderItemId}")
    public ResponseEntity<?> removeOrderItem(
            @PathVariable Long orderId,
            @PathVariable Long orderItemId,
            @RequestParam(required = false) String reason
    ) {
        try {
            OrderDTO order = orderService.adminRemoveOrderItem(orderId, orderItemId, reason);
            log.info("✅ Admin a supprimé l'item #{} de la commande #{} - Raison: {}",
                    orderItemId, orderId, reason);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Plat supprimé avec succès");
            response.put("order", order);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("❌ Erreur suppression item #{}: {}", orderItemId, e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * ✏️ MODIFIE LA QUANTITÉ D'UN PLAT
     */
    @PutMapping("/{orderId}/items/{orderItemId}/quantity")
    public ResponseEntity<?> updateOrderItemQuantity(
            @PathVariable Long orderId,
            @PathVariable Long orderItemId,
            @RequestBody OrderItemUpdateRequest request
    ) {
        try {
            OrderDTO order = orderService.adminUpdateOrderItemQuantity(
                    orderId, orderItemId, request.getNewQuantity(), request.getReason()
            );

            log.info("✅ Admin a modifié la quantité de l'item #{} - Nouvelle quantité: {} - Raison: {}",
                    orderItemId, request.getNewQuantity(), request.getReason());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Quantité modifiée avec succès");
            response.put("order", order);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("❌ Erreur modification quantité item #{}: {}", orderItemId, e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 🔄 RÉACTIVE UNE COMMANDE ANNULÉE
     */
    @PutMapping("/{orderId}/reactivate")
    public ResponseEntity<?> reactivateOrder(
            @PathVariable Long orderId,
            @RequestParam(required = false) String reason
    ) {
        try {
            OrderDTO order = orderService.adminReactivateOrder(orderId, reason);
            log.info("✅ Admin a réactivé la commande #{} - Raison: {}", orderId, reason);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Commande réactivée avec succès");
            response.put("order", order);
            response.put("chargedAmount", order.getTotalAmount());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("❌ Erreur réactivation commande #{}: {}", orderId, e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * ➕ CRÉE OU REMPLACE UNE COMMANDE POUR UN UTILISATEUR (sans contrainte horaire)
     */
    @PostMapping("/for-user")
    public ResponseEntity<?> adminCreateOrder(@RequestBody AdminCreateOrderRequest request) {
        try {
            OrderDTO order = orderService.adminCreateOrder(request);
            log.info("✅ Admin a créé/remplacé une commande pour l'utilisateur #{} le {}", request.getUserId(), request.getDate());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Commande créée avec succès pour " + order.getUsername());
            response.put("order", order);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("❌ Erreur création commande admin: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 🚚 DÉCLENCHE MANUELLEMENT LE CALCUL DES FRAIS
     */
    @PostMapping("/calculate-delivery-fees")
    public ResponseEntity<String> triggerDeliveryFeeCalculation() {
        deliveryFeeService.triggerManualCalculation();
        return ResponseEntity.ok("Calcul des frais de livraison lancé avec succès");
    }

    /**
     * 🛠️ Crée une réponse d'erreur standardisée
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", message);
        error.put("timestamp", java.time.LocalDateTime.now().toString());
        return error;
    }
}
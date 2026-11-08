package heritage.africca.accel_connect.controller;

import heritage.africca.accel_connect.dto.OrderCreationPayload;
import heritage.africca.accel_connect.dto.OrderDTO;
import heritage.africca.accel_connect.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
@CrossOrigin("*")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    /**
     * 🛒 CRÉE LES COMMANDES À PARTIR DU PANIER
     */
    @PostMapping
    public ResponseEntity<?> createOrders(
            Authentication auth,
            @RequestBody OrderCreationPayload payload) {
        try {
            OrderDTO order = orderService.createOrder(auth, payload);
            log.info("Commande créée avec succès: {}", order.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(Collections.singletonList(order));
        } catch (RuntimeException e) {
            log.warn("Erreur création commande: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 📋 RÉCUPÈRE TOUTES LES COMMANDES DE L'UTILISATEUR
     */
    @GetMapping
    public ResponseEntity<List<OrderDTO>> getMyOrders(Authentication auth) {
        try {
            List<OrderDTO> orders = orderService.getMyOrders(auth);
            return ResponseEntity.ok(orders);
        } catch (RuntimeException e) {
            log.error("Erreur récupération commandes: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * ❌ ANNULE UNE COMMANDE
     */
    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(
            Authentication auth,
            @PathVariable Long orderId) {
        try {
            OrderDTO order = orderService.cancelOrder(auth, orderId);
            log.info("Commande annulée: {}", orderId);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            log.warn("Erreur annulation commande: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 🛠️ Crée une réponse d'erreur standardisée
     */
    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        error.put("timestamp", java.time.LocalDateTime.now().toString());
        return error;
    }
}
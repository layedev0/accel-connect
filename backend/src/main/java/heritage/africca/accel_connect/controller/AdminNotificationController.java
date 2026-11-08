package heritage.africca.accel_connect.controller;

import heritage.africca.accel_connect.entity.Order;
import heritage.africca.accel_connect.entity.Restaurant;
import heritage.africca.accel_connect.entity.User;
import heritage.africca.accel_connect.repository.OrderRepository;
import heritage.africca.accel_connect.repository.RestaurantRepository;
import heritage.africca.accel_connect.repository.UserRepository;
import heritage.africca.accel_connect.service.Impl.NextcloudTalkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/notify")
@CrossOrigin("*")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')")
@Slf4j
public class AdminNotificationController {

    private final NextcloudTalkService nextcloudTalkService;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final RestaurantRepository restaurantRepository;

    /**
     * 🍽️ NOTIFIE TOUS LES UTILISATEURS que les menus du jour sont disponibles.
     * Retourne immédiatement, traitement en arrière-plan.
     */
    @PostMapping("/menu-available")
    public ResponseEntity<Map<String, Object>> notifyMenuAvailable() {
        List<User> allUsers = userRepository.findAll();
        String message = "🍽️ Les menus du jour sont disponibles ! Merci de commander avant 10h.";

        log.info("🔔 Envoi notifications menus à {} utilisateurs (arrière-plan)", allUsers.size());

        // Copie pour le thread async (évite problème de lazy loading)
        List<Map.Entry<String, String>> targets = allUsers.stream()
                .map(u -> Map.entry("SSO-" + u.getKeycloakId(), message))
                .collect(Collectors.toList());

        CompletableFuture.runAsync(() -> {
            int sent = 0, failed = 0;
            for (Map.Entry<String, String> t : targets) {
                if (nextcloudTalkService.sendDirectMessage(t.getKey(), t.getValue())) sent++;
                else failed++;
            }
            log.info("✅ Notifications menus terminées : {}/{} envoyés", sent, targets.size());
        });

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("total", allUsers.size());
        result.put("message", "Notifications en cours d'envoi à " + allUsers.size() + " utilisateur(s)");
        return ResponseEntity.ok(result);
    }

    /**
     * 🚚 NOTIFIE LES UTILISATEURS d'un restaurant que leur commande est prête.
     * Retourne immédiatement, traitement en arrière-plan.
     */
    @PostMapping("/restaurant-delivered/{restaurantId}")
    public ResponseEntity<Map<String, Object>> notifyRestaurantDelivered(
            @PathVariable Long restaurantId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate targetDate = date != null ? date : LocalDate.now();

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Restaurant #" + restaurantId + " introuvable"));

        List<Order> orders = orderRepository.findConfirmedByRestaurantIdAndDate(restaurantId, targetDate);

        log.info("🔔 Envoi notifications livraison '{}' à {} commandes (arrière-plan)",
                restaurant.getRestaurantName(), orders.size());

        String restaurantName = restaurant.getRestaurantName();

        // Préparer tous les messages AVANT le thread async (évite lazy loading hors transaction)
        List<Map.Entry<String, String>> targets = new ArrayList<>();
        for (Order order : orders) {
            List<String> mealNames = order.getOrderItems().stream()
                    .filter(item -> item.getRestaurant().getId().equals(restaurantId))
                    .map(item -> item.getQuantity() > 1
                            ? item.getQuantity() + "x " + item.getMeal().getMealName()
                            : item.getMeal().getMealName())
                    .collect(Collectors.toList());

            if (mealNames.isEmpty()) continue;

            String msg = "🍱 Votre commande " + restaurantName + " (" + String.join(", ", mealNames) + ") est disponible. "
                    + "Nous vous prions de bien vouloir vérifier que ce que vous prenez est bien votre commande. 🙏";

            targets.add(Map.entry("SSO-" + order.getUser().getKeycloakId(), msg));
        }

        CompletableFuture.runAsync(() -> {
            int sent = 0, failed = 0;
            for (Map.Entry<String, String> t : targets) {
                if (nextcloudTalkService.sendDirectMessage(t.getKey(), t.getValue())) sent++;
                else failed++;
            }
            log.info("✅ Notifications livraison '{}' terminées : {}/{} envoyés", restaurantName, sent, targets.size());
        });

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("restaurantName", restaurantName);
        result.put("total", targets.size());
        result.put("message", "Notifications en cours d'envoi pour " + targets.size() + " commande(s)");
        return ResponseEntity.ok(result);
    }
}

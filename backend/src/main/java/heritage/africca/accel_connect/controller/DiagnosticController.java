package heritage.africca.accel_connect.controller;

import heritage.africca.accel_connect.entity.Order;
import heritage.africca.accel_connect.entity.Restaurant;
import heritage.africca.accel_connect.entity.User;
import heritage.africca.accel_connect.entity.UserBalance;
import heritage.africca.accel_connect.entity.enumeration.OrderStatus;
import heritage.africca.accel_connect.repository.OrderRepository;
import heritage.africca.accel_connect.repository.RestaurantRepository;
import heritage.africca.accel_connect.repository.UserBalanceRepository;
import heritage.africca.accel_connect.repository.UserRepository;
import heritage.africca.accel_connect.service.DeliveryFeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 🔍 ENDPOINT DE DIAGNOSTIC - POUR DEBUGGER LES FRAIS DE LIVRAISON
 * À SUPPRIMER EN PRODUCTION!
 */
@RestController
@RequestMapping("/api/diagnostic")
@RequiredArgsConstructor
@Slf4j
public class DiagnosticController {

    private final OrderRepository orderRepository;
    private final RestaurantRepository restaurantRepository;
    private final DeliveryFeeService deliveryFeeService;
    private final UserRepository userRepository;
    private final UserBalanceRepository userBalanceRepository;

    /**
     * 🔍 Affiche l'état des commandes du jour
     */
    @GetMapping("/today-orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getTodayOrdersStatus() {
        LocalDate today = LocalDate.now();

        List<Order> orders = orderRepository.findByOrderDateBetweenAndStatus(
                today, today, OrderStatus.CONFIRMED
        );

        Map<String, Object> response = new HashMap<>();
        response.put("date", today.toString());
        response.put("totalOrders", orders.size());

        List<Map<String, Object>> orderDetails = orders.stream()
                .map(order -> {
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("orderId", order.getId());
                    detail.put("userId", order.getUser().getId());
                    detail.put("username", order.getUser().getUsername());
                    detail.put("subtotal", order.getSubtotal());
                    detail.put("amountAfterSubsidy", order.getAmountAfterSubsidy());
                    detail.put("deliveryFeeShare", order.getDeliveryFeeShare());
                    detail.put("totalAmount", order.getTotalAmount());

                    List<String> restaurants = order.getOrderItems().stream()
                            .map(item -> item.getRestaurant().getRestaurantName())
                            .distinct()
                            .toList();
                    detail.put("restaurants", restaurants);

                    return detail;
                })
                .toList();

        response.put("orders", orderDetails);

        return ResponseEntity.ok(response);
    }

    /**
     * 🔍 Affiche les frais de livraison configurés pour chaque restaurant
     */
    @GetMapping("/restaurant-fees")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getRestaurantFees() {
        List<Restaurant> restaurants = (List<Restaurant>) restaurantRepository.findAll();

        List<Map<String, Object>> response = restaurants.stream()
                .map(restaurant -> {
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("id", restaurant.getId());
                    detail.put("name", restaurant.getRestaurantName());
                    detail.put("deliveryFee", restaurant.getDeliveryFee());
                    return detail;
                })
                .toList();

        return ResponseEntity.ok(response);
    }

    /**
     * 🔄 Force le recalcul des frais pour une date donnée
     */
    @PostMapping("/recalculate/{date}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> forceRecalculate(@PathVariable String date) {
        try {
            LocalDate targetDate = LocalDate.parse(date);
            log.info("🔧 RECALCUL FORCÉ demandé pour le {}", targetDate);

            deliveryFeeService.recalculateDeliveryFeesForDate(targetDate);

            return ResponseEntity.ok("✅ Recalcul terminé pour le " + targetDate);
        } catch (Exception e) {
            log.error("❌ Erreur lors du recalcul forcé", e);
            return ResponseEntity.badRequest().body("❌ Erreur: " + e.getMessage());
        }
    }

    /**
     * 🔄 Force le recalcul pour aujourd'hui
     */
    @PostMapping("/recalculate-today")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> forceRecalculateToday() {
        try {
            LocalDate today = LocalDate.now();
            log.info("🔧 RECALCUL FORCÉ demandé pour aujourd'hui {}", today);

            deliveryFeeService.recalculateDeliveryFeesForDate(today);

            return ResponseEntity.ok("✅ Recalcul terminé pour aujourd'hui");
        } catch (Exception e) {
            log.error("❌ Erreur lors du recalcul forcé", e);
            return ResponseEntity.badRequest().body("❌ Erreur: " + e.getMessage());
        }
    }

    /**
     * 🔄 Recalcule les commandes d'aujourd'hui pour les stagiaires
     * Change la subvention de 1000 à 2000 FCFA et rembourse la différence
     */
    @PostMapping("/recalculate-intern-subsidy-today")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Map<String, Object>> recalculateInternSubsidyToday() {
        try {
            LocalDate today = LocalDate.now();
            log.info("🔄 Recalcul subvention stagiaires pour le {}", today);

            List<Order> todayOrders = orderRepository.findByOrderDateBetweenAndStatus(
                    today, today, OrderStatus.CONFIRMED
            );

            int updated = 0;
            double totalRefunded = 0;
            List<Map<String, Object>> details = new java.util.ArrayList<>();

            for (Order order : todayOrders) {
                User user = order.getUser();
                if (!Boolean.TRUE.equals(user.getIntern())) {
                    continue; // Pas un stagiaire, on skip
                }

                double oldSubsidy = order.getSubsidyAmount();
                if (oldSubsidy >= 2000.0) {
                    continue; // Déjà à 2000, rien à faire
                }

                double newSubsidy = 2000.0;
                double oldAmountAfterSubsidy = order.getAmountAfterSubsidy();
                double newAmountAfterSubsidy = Math.max(0, order.getSubtotal() - newSubsidy);
                double refund = oldAmountAfterSubsidy - newAmountAfterSubsidy;

                // Mettre à jour la commande
                order.setSubsidyAmount(newSubsidy);
                order.setAmountAfterSubsidy(newAmountAfterSubsidy);
                order.setTotalAmount(newAmountAfterSubsidy + order.getDeliveryFeeShare());
                orderRepository.save(order);

                // Rembourser la différence sur le solde
                if (refund > 0) {
                    UserBalance balance = userBalanceRepository.findByUserId(user.getId()).orElse(null);
                    if (balance != null) {
                        balance.setTotalSpent(balance.getTotalSpent() - refund);
                        balance.setBalance(balance.getTotalDeposited() - balance.getTotalSpent());
                        userBalanceRepository.save(balance);
                    }
                }

                Map<String, Object> detail = new HashMap<>();
                detail.put("user", user.getFirstName() + " " + user.getLastName());
                detail.put("orderId", order.getId());
                detail.put("subtotal", order.getSubtotal());
                detail.put("oldSubsidy", oldSubsidy);
                detail.put("newSubsidy", newSubsidy);
                detail.put("oldAmountAfterSubsidy", oldAmountAfterSubsidy);
                detail.put("newAmountAfterSubsidy", newAmountAfterSubsidy);
                detail.put("refund", refund);
                details.add(detail);

                updated++;
                totalRefunded += refund;

                log.info("✅ {} {} : subvention {} -> {}, remboursé {} FCFA",
                        user.getFirstName(), user.getLastName(), oldSubsidy, newSubsidy, refund);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("date", today.toString());
            result.put("ordersUpdated", updated);
            result.put("totalRefunded", totalRefunded);
            result.put("details", details);
            result.put("status", "SUCCESS");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Erreur recalcul subvention stagiaires", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "ERROR");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 🔄 RESET COMPLET ET RECALCUL DEPUIS ZÉRO
     * ⚠️ ATTENTION: Ceci va RESET tous les UserBalance et les recalculer depuis les commandes
     * Utilisez ceci UNIQUEMENT pour corriger des données corrompues!
     */
    @PostMapping("/reset-and-recalculate-week")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Map<String, Object>> resetAndRecalculateWeek() {
        try {
            log.warn("🚨 RESET COMPLET ET RECALCUL DEMANDÉ - OPÉRATION DANGEREUSE!");

            LocalDate today = LocalDate.now();
            LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate friday = monday.plusDays(4);

            Map<String, Object> result = new HashMap<>();
            result.put("weekStart", monday.toString());
            result.put("weekEnd", friday.toString());

            // ÉTAPE 1: Récupérer TOUTES les commandes CONFIRMED de la semaine
            List<Order> allWeekOrders = orderRepository.findByOrderDateBetweenAndStatus(
                    monday, friday, OrderStatus.CONFIRMED
            );

            log.info("📊 {} commandes trouvées pour la semaine", allWeekOrders.size());

            // ÉTAPE 2: RESET tous les frais de livraison sur les commandes
            for (Order order : allWeekOrders) {
                order.setDeliveryFeeShare(0.0);
                order.setTotalAmount(order.getAmountAfterSubsidy());
            }
            orderRepository.saveAll(allWeekOrders);
            log.info("✅ Tous les frais réinitialisés sur les commandes");

            // ÉTAPE 3: RESET tous les UserBalance à partir des dépôts uniquement
            List<User> allUsers = (List<User>) userRepository.findAll();
            for (User user : allUsers) {
                UserBalance balance = userBalanceRepository.findByUserId(user.getId())
                        .orElseGet(() -> {
                            UserBalance newBalance = new UserBalance();
                            newBalance.setUser(user);
                            newBalance.setTotalDeposited(0.0);
                            return newBalance;
                        });

                // Garder totalDeposited, reset totalSpent
                double deposits = balance.getTotalDeposited();
                balance.setTotalSpent(0.0);
                balance.setBalance(deposits);

                userBalanceRepository.save(balance);
            }
            log.info("✅ Tous les UserBalance réinitialisés");

            // ÉTAPE 4: Recalculer totalSpent à partir des commandes
            for (Order order : allWeekOrders) {
                UserBalance balance = userBalanceRepository.findByUserId(order.getUser().getId())
                        .orElseThrow(() -> new RuntimeException("Balance introuvable"));

                // Ajouter le montant de la commande (sans frais pour l'instant)
                balance.addExpense(order.getAmountAfterSubsidy());
                userBalanceRepository.save(balance);
            }
            log.info("✅ totalSpent recalculé depuis les commandes");

            // ÉTAPE 5: Recalculer les frais jour par jour
            int daysProcessed = 0;
            LocalDate currentDate = monday;
            while (!currentDate.isAfter(friday)) {
                deliveryFeeService.recalculateDeliveryFeesForDate(currentDate);
                currentDate = currentDate.plusDays(1);
                daysProcessed++;
            }

            result.put("daysProcessed", daysProcessed);
            result.put("ordersProcessed", allWeekOrders.size());
            result.put("usersProcessed", allUsers.size());
            result.put("status", "SUCCESS");
            result.put("message", "✅ RESET et recalcul terminés avec succès!");

            log.info("🎉 RESET COMPLET TERMINÉ - {} jours, {} commandes, {} utilisateurs",
                    daysProcessed, allWeekOrders.size(), allUsers.size());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Erreur lors du reset complet", e);
            Map<String, Object> error = new HashMap<>();
            error.put("status", "ERROR");
            error.put("message", "❌ Erreur: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}

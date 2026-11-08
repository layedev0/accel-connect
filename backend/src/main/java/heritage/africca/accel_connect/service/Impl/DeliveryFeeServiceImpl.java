package heritage.africca.accel_connect.service.Impl;

import heritage.africca.accel_connect.entity.*;
import heritage.africca.accel_connect.entity.enumeration.OrderStatus;
import heritage.africca.accel_connect.repository.*;
import heritage.africca.accel_connect.service.DeliveryFeeService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * 🚚 SERVICE DE CALCUL DES FRAIS DE LIVRAISON
 *
 * LOGIQUE:
 * 1. Chaque vendredi à 15h, calcul automatique des frais de la semaine
 * 2. Pour chaque restaurant, on groupe les utilisateurs qui ont commandé
 * 3. On calcule la part de chaque utilisateur:
 *    Part = (frais_par_jour × nb_jours_utilisateur) ÷ nb_utilisateurs_uniques
 * 4. On met à jour les commandes et on débite les UserBalance
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DeliveryFeeServiceImpl implements DeliveryFeeService {

    private final OrderRepository orderRepository;
    private final RestaurantRepository restaurantRepository;
    private final UserBalanceRepository userBalanceRepository;
    private final DeliveryFeeCalculationRepository deliveryFeeCalculationRepository;

    /**
     * 🕒 TÂCHE PLANIFIÉE: Chaque VENDREDI à 15h00
     * Calcule automatiquement les frais de livraison de la semaine
     */
    @Scheduled(cron = "0 0 15 * * FRI", zone = "Africa/Dakar")
    @Transactional
    @Override
    public void calculateWeeklyDeliveryFees() {
        log.info("🚚 === DÉBUT DU CALCUL AUTOMATIQUE DES FRAIS DE LIVRAISON ===");

        LocalDate monday = getCurrentMonday();
        LocalDate friday = getCurrentFriday();

        calculateDeliveryFeesForPeriod(monday, friday);

        log.info("✅ === FIN DU CALCUL AUTOMATIQUE DES FRAIS DE LIVRAISON ===");
    }

    /**
     * 🔧 CALCUL POUR UNE PÉRIODE SPÉCIFIQUE (utilisé par validation paiement)
     * Cette méthode est appelée lors de la validation des paiements
     */
    @Transactional
    public void calculateDeliveryFeesForPeriod(LocalDate startDate, LocalDate endDate) {
        log.info("📅 Calcul des frais de livraison du {} au {}", startDate, endDate);

        // 1️⃣ Récupérer toutes les commandes CONFIRMÉES de la période
        List<Order> weekOrders = orderRepository.findByOrderDateBetweenAndStatus(
                startDate, endDate, OrderStatus.CONFIRMED
        );

        if (weekOrders.isEmpty()) {
            log.info("ℹ️ Aucune commande confirmée pour la période");
            return;
        }

        log.info("📦 {} commande(s) confirmée(s) trouvée(s)", weekOrders.size());

        // 2️⃣ Grouper par RESTAURANT
        Map<Restaurant, List<OrderItemWithUser>> itemsByRestaurant = new HashMap<>();

        for (Order order : weekOrders) {
            for (OrderItem item : order.getOrderItems()) {
                Restaurant restaurant = item.getRestaurant();

                itemsByRestaurant.computeIfAbsent(restaurant, k -> new ArrayList<>())
                        .add(new OrderItemWithUser(item, order.getUser(), order.getOrderDate()));
            }
        }

        // 3️⃣ Traiter chaque restaurant
        itemsByRestaurant.forEach((restaurant, items) ->
                processRestaurantDeliveryFees(restaurant, items, startDate)
        );

        log.info("✅ Frais de livraison calculés pour {} restaurant(s)", itemsByRestaurant.size());
    }

    /**
     * 🏪 TRAITE LES FRAIS DE LIVRAISON POUR UN RESTAURANT
     */
    private void processRestaurantDeliveryFees(
            Restaurant restaurant,
            List<OrderItemWithUser> items,
            LocalDate weekStart
    ) {
        log.info("🏪 Restaurant: {}", restaurant.getRestaurantName());

        Double dailyFee = restaurant.getDeliveryFee();

        // ✅ CAS 1: Livraison GRATUITE
        if (dailyFee == null || dailyFee <= 0) {
            log.info("🎉 Livraison GRATUITE - Aucun frais à répartir");
            return;
        }

        // 💰 CAS 2: Frais de livraison à répartir
        log.info("💰 Frais par jour: {} FCFA", dailyFee);

        // 👥 Compter les JOURS où chaque utilisateur a commandé
        Map<User, Set<LocalDate>> userOrderDays = new HashMap<>();

        for (OrderItemWithUser item : items) {
            userOrderDays.computeIfAbsent(item.user, k -> new HashSet<>())
                    .add(item.orderDate);
        }

        // 📊 Nombre d'utilisateurs uniques
        int totalUsers = userOrderDays.size();
        log.info("👥 {} utilisateur(s) distinct(s)", totalUsers);

        // 💵 Calculer et appliquer les frais pour chaque utilisateur
        userOrderDays.forEach((user, orderDays) -> {
            int daysCount = orderDays.size();

            // Part = (frais_par_jour × nb_jours) ÷ nb_utilisateurs
            Double userShare = Math.round(((dailyFee * daysCount) / totalUsers) * 100.0) / 100.0;

            log.info("  👤 {} - {} jour(s) - Part: {} FCFA",
                    user.getUsername(), daysCount, userShare);

            applyDeliveryFeesToUser(user, restaurant, userShare, orderDays,
                    dailyFee, totalUsers, weekStart);
        });
    }

    /**
     * 💳 APPLIQUE LES FRAIS À UN UTILISATEUR
     */
    private void applyDeliveryFeesToUser(
            User user,
            Restaurant restaurant,
            Double userShare,
            Set<LocalDate> orderDays,
            Double dailyFee,
            int totalUsers,
            LocalDate weekStart
    ) {
        // 1️⃣ Mettre à jour TOUTES les commandes de l'utilisateur pour cette période
        Double feePerDay = Math.round((userShare / orderDays.size()) * 100.0) / 100.0;

        for (LocalDate orderDate : orderDays) {
            Optional<Order> orderOpt = orderRepository.findByUserAndOrderDate(user, orderDate);
            if (orderOpt.isPresent()) {
                Order order = orderOpt.get();

                // Calculer la proportion des frais pour ce restaurant dans cette commande
                double restaurantSubtotal = order.getOrderItems().stream()
                        .filter(item -> item.getRestaurant().getId().equals(restaurant.getId()))
                        .mapToDouble(OrderItem::getSubtotal)
                        .sum();

                double orderSubtotal = order.getSubtotal();
                double proportion = orderSubtotal > 0 ? restaurantSubtotal / orderSubtotal : 0;

                // Ajouter les frais proportionnels
                Double restaurantFee = Math.round((feePerDay * proportion) * 100.0) / 100.0;
                order.addDeliveryFees(restaurantFee);
                orderRepository.save(order);

                log.debug("  📦 Commande #{} - Frais ajoutés: {} FCFA", order.getId(), restaurantFee);
            }
        }

        // 2️⃣ Débiter le UserBalance
        UserBalance balance = userBalanceRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Balance non trouvée pour user " + user.getId()));

        balance.addExpense(userShare);
        userBalanceRepository.save(balance);

        log.info("  💳 UserBalance débité de {} FCFA - Nouveau solde: {} FCFA",
                userShare, balance.getBalance());

        // 3️⃣ Enregistrer le calcul pour traçabilité
        DeliveryFeeCalculation calculation = new DeliveryFeeCalculation();
        calculation.setUser(user);
        calculation.setRestaurant(restaurant);
        calculation.setWeekStartDate(weekStart);
        calculation.setOrderDaysCount(orderDays.size());
        calculation.setTotalUsers(totalUsers);
        calculation.setRestaurantDailyFee(dailyFee);
        calculation.setUserShare(userShare);
        deliveryFeeCalculationRepository.save(calculation);
    }

    /**
     * 🔧 MÉTHODE MANUELLE (pour tests ou admin)
     * NE DOIT ÊTRE UTILISÉE QUE LE VENDREDI
     */
    @Transactional
    @Override
    public void triggerManualCalculation() {
        LocalDate today = LocalDate.now();

        if (today.getDayOfWeek() != DayOfWeek.FRIDAY) {
            throw new RuntimeException("⚠️ Le calcul manuel des frais de livraison ne peut être effectué que le VENDREDI");
        }

        log.info("🔧 Déclenchement MANUEL du calcul des frais");
        calculateWeeklyDeliveryFees();
    }

    // ========== UTILITAIRES ==========

    private LocalDate getCurrentMonday() {
        return LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private LocalDate getCurrentFriday() {
        return LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));
    }

    /**
     * ✅ Vérifie si on est vendredi
     */
    public boolean isFriday() {
        return LocalDate.now().getDayOfWeek() == DayOfWeek.FRIDAY;
    }

    /**
     * 📅 Retourne la semaine courante (lundi-vendredi)
     */
    public Map<String, LocalDate> getCurrentWeekPeriod() {
        Map<String, LocalDate> period = new HashMap<>();
        period.put("monday", getCurrentMonday());
        period.put("friday", getCurrentFriday());
        return period;
    }

    /**
     * 💰 CALCULE LES FRAIS DE LIVRAISON POUR UN UTILISATEUR
     * Cette méthode lit simplement delivery_fee_share des commandes
     * (qui sont maintenant calculés en temps réel à chaque confirmation)
     *
     * @param userId L'ID de l'utilisateur
     * @return Le montant total des frais de livraison pour la semaine
     */
    @Transactional(readOnly = true)
    @Override
    public Double calculateEstimatedDeliveryFees(Long userId) {
        LocalDate monday = getCurrentMonday();
        LocalDate friday = getCurrentFriday();

        // Récupérer toutes les commandes CONFIRMÉES de l'utilisateur cette semaine
        List<Order> userOrders = orderRepository.findByUserIdAndOrderDateBetweenAndStatus(
                userId, monday, friday, OrderStatus.CONFIRMED
        );

        if (userOrders.isEmpty()) {
            return 0.0;
        }

        // Somme des delivery_fee_share déjà calculés
        double totalFees = userOrders.stream()
                .filter(order -> order.getDeliveryFeeShare() != null)
                .mapToDouble(Order::getDeliveryFeeShare)
                .sum();

        log.debug("💰 Frais de livraison totaux pour user {}: {} FCFA", userId, totalFees);
        return Math.round(totalFees * 100.0) / 100.0;
    }


    /**
     * 🔄 RECALCULE LES FRAIS DE LIVRAISON POUR UNE DATE DONNÉE
     * Cette méthode est appelée automatiquement quand une commande est confirmée
     * Elle recalcule les frais pour TOUTES les commandes de ce jour dans chaque restaurant
     */
    @Transactional
    @Override
    public void recalculateDeliveryFeesForDate(LocalDate orderDate) {
        log.info("🔄 Recalcul des frais de livraison pour le {}", orderDate);

        // Récupérer toutes les commandes confirmées de ce jour
        List<Order> ordersOfDay = orderRepository.findByOrderDateBetweenAndStatus(
                orderDate, orderDate, OrderStatus.CONFIRMED
        );

        if (ordersOfDay.isEmpty()) {
            log.info("Aucune commande confirmée pour le {}", orderDate);
            return;
        }

        // 🔄 ÉTAPE 1: Sauvegarder les anciens frais AVANT de réinitialiser
        Map<Long, Double> oldFees = new HashMap<>();
        for (Order order : ordersOfDay) {
            oldFees.put(order.getId(), order.getDeliveryFeeShare() != null ? order.getDeliveryFeeShare() : 0.0);
            order.setDeliveryFeeShare(0.0);
            order.setTotalAmount(order.getAmountAfterSubsidy());
        }

        // NE PAS SAUVEGARDER ICI - on sauvegarde tout à la fin!

        // 🏪 ÉTAPE 2: Trouver tous les restaurants uniques
        Set<Restaurant> restaurants = new HashSet<>();
        for (Order order : ordersOfDay) {
            for (OrderItem item : order.getOrderItems()) {
                restaurants.add(item.getRestaurant());
            }
        }

        // 💰 ÉTAPE 3: Pour chaque restaurant, calculer et ADDITIONNER les frais
        for (Restaurant restaurant : restaurants) {
            Double dailyFee = restaurant.getDeliveryFee();

            if (dailyFee == null || dailyFee <= 0) {
                log.debug("Restaurant {} - pas de frais de livraison", restaurant.getRestaurantName());
                continue;
            }

            // Trouver tous les utilisateurs UNIQUES qui ont commandé dans CE restaurant CE JOUR
            Set<Long> uniqueUserIds = new HashSet<>();
            for (Order order : ordersOfDay) {
                boolean hasItemFromThisRestaurant = order.getOrderItems().stream()
                        .anyMatch(item -> item.getRestaurant().getId().equals(restaurant.getId()));

                if (hasItemFromThisRestaurant) {
                    uniqueUserIds.add(order.getUser().getId());
                }
            }

            int totalUsers = uniqueUserIds.size();
            if (totalUsers == 0) continue;

            // Calculer la part par utilisateur pour CE restaurant
            Double feePerUser = Math.round((dailyFee / totalUsers) * 100.0) / 100.0;

            log.info("📊 {} le {} - {} FCFA ÷ {} utilisateurs = {} FCFA/personne",
                    restaurant.getRestaurantName(), orderDate, dailyFee, totalUsers, feePerUser);

            // ➕ ADDITIONNER les frais pour chaque commande concernée
            for (Order order : ordersOfDay) {
                boolean hasItemFromThisRestaurant = order.getOrderItems().stream()
                        .anyMatch(item -> item.getRestaurant().getId().equals(restaurant.getId()));

                if (hasItemFromThisRestaurant) {
                    Double currentFee = order.getDeliveryFeeShare();
                    Double newFee = currentFee + feePerUser;

                    // ADDITIONNER les frais (pas écraser!)
                    order.setDeliveryFeeShare(newFee);
                    order.setTotalAmount(order.getAmountAfterSubsidy() + newFee);

                    log.debug("✅ Commande #{} - User {} - Frais {} ajouté (+{} FCFA) = {} FCFA total",
                            order.getId(), order.getUser().getUsername(),
                            restaurant.getRestaurantName(), feePerUser, newFee);
                }
            }
        }

        // 🔄 ÉTAPE 4: Ajuster les UserBalance en fonction de la DIFFÉRENCE
        for (Order order : ordersOfDay) {
            Double oldFee = oldFees.get(order.getId());
            Double newFee = order.getDeliveryFeeShare();
            Double difference = newFee - oldFee;

            if (Math.abs(difference) > 0.01) { // Tolérance pour les arrondis
                UserBalance balance = userBalanceRepository.findByUserId(order.getUser().getId())
                        .orElseThrow(() -> new RuntimeException("Balance non trouvée"));

                if (difference > 0) {
                    // Augmentation des frais
                    balance.addExpense(difference);
                    log.debug("💳 Débit {} FCFA pour user {} (frais: {} → {})",
                            difference, order.getUser().getUsername(), oldFee, newFee);
                } else {
                    // Diminution des frais (remboursement)
                    balance.refundExpense(Math.abs(difference));
                    log.debug("💰 Remboursement {} FCFA pour user {} (frais: {} → {})",
                            Math.abs(difference), order.getUser().getUsername(), oldFee, newFee);
                }

                userBalanceRepository.save(balance);
            }
        }

        // 💾 SAUVEGARDER toutes les commandes
        orderRepository.saveAll(ordersOfDay);
        log.info("✅ Recalcul terminé pour le {} - {} restaurants traités",
                orderDate, restaurants.size());
    }

    /**
     * Classe helper pour grouper les items avec leur utilisateur et date
     */
    @AllArgsConstructor
    private static class OrderItemWithUser {
        OrderItem item;
        User user;
        LocalDate orderDate;
    }
}
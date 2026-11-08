package heritage.africca.accel_connect.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import heritage.africca.accel_connect.dto.*;
import heritage.africca.accel_connect.entity.*;
import heritage.africca.accel_connect.entity.enumeration.OrderStatus;
import heritage.africca.accel_connect.repository.*;
import heritage.africca.accel_connect.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final OrderRepository orderRepository;
    private final RestaurantRepository restaurantRepository;
    private final UserBalanceRepository userBalanceRepository;
    private final DeliveryFeeCalculationRepository deliveryFeeCalculationRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final ObjectMapper objectMapper;
    private final DeliveryFeeServiceImpl deliveryFeeService;

    private static final double SUBSIDY_AMOUNT = 1000.0;

    /**
     * 📊 Génère le récapitulatif de paiement pour la semaine en cours
     * ⚠️ NE PEUT ÊTRE APPELÉ QUE LE VENDREDI
     */
    @Override
    @Transactional(readOnly = true)
    public PaymentSummary generatePaymentSummary(LocalDate startDate, LocalDate endDate) {
        log.info("📊 Génération du récapitulatif de paiement du {} au {}", startDate, endDate);

        // ✅ VÉRIFICATION: On doit être vendredi
        if (LocalDate.now().getDayOfWeek() != DayOfWeek.FRIDAY) {
            throw new RuntimeException("⚠️ Le récapitulatif de paiement ne peut être généré que le VENDREDI");
        }

        // 📅 FORCER LA PÉRIODE À LA SEMAINE EN COURS (Lundi-Vendredi)
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate friday = LocalDate.now(); // Aujourd'hui = Vendredi

        log.info("📅 Période forcée à la semaine courante: {} au {}", monday, friday);

        // Récupérer le dernier paiement
        Optional<PaymentRecord> lastPayment = paymentRecordRepository.findLastPayment();

        // Vérifier qu'on n'a pas déjà payé cette semaine
        if (lastPayment.isPresent() && lastPayment.get().getEndDate().equals(friday)) {
            throw new RuntimeException("⚠️ La semaine du " + monday + " au " + friday + " a déjà été payée");
        }

        // Récupérer toutes les commandes confirmées de la semaine
        List<Order> orders = orderRepository.findByOrderDateBetweenAndStatus(
                monday, friday, OrderStatus.CONFIRMED
        );

        if (orders.isEmpty()) {
            log.info("ℹ️ Aucune commande confirmée cette semaine");
            return createEmptyPaymentSummary(monday, friday, lastPayment);
        }

        log.info("📦 {} commande(s) trouvée(s)", orders.size());

        // Grouper par restaurant
        Map<Restaurant, List<Order>> ordersByRestaurant = new HashMap<>();
        Set<Long> uniqueUsers = new HashSet<>();

        for (Order order : orders) {
            uniqueUsers.add(order.getUser().getId());
            for (OrderItem item : order.getOrderItems()) {
                Restaurant restaurant = item.getRestaurant();
                ordersByRestaurant.computeIfAbsent(restaurant, k -> new ArrayList<>())
                        .add(order);
            }
        }

        // Calculer les statistiques par restaurant
        List<RestaurantPaymentSummary> restaurantSummaries = new ArrayList<>();
        double totalWithSubsidy = 0.0;
        double totalWithoutSubsidy = 0.0;
        double totalSubsidy = 0.0;
        double totalDeliveryFees = 0.0;

        for (Map.Entry<Restaurant, List<Order>> entry : ordersByRestaurant.entrySet()) {
            Restaurant restaurant = entry.getKey();
            List<Order> restaurantOrders = entry.getValue().stream()
                    .distinct()
                    .collect(Collectors.toList());

            RestaurantPaymentSummary summary = calculateRestaurantSummary(
                    restaurant, restaurantOrders, monday, friday
            );

            restaurantSummaries.add(summary);
            totalWithSubsidy += summary.getTotalAmountWithSubsidy();
            totalWithoutSubsidy += summary.getTotalAmountWithoutSubsidy();
            totalSubsidy += summary.getSubsidyAmount();
            totalDeliveryFees += summary.getDeliveryFees();
        }

        // Créer le récapitulatif global
        PaymentSummary paymentSummary = new PaymentSummary();
        paymentSummary.setStartDate(monday.toString());
        paymentSummary.setEndDate(friday.toString());
        paymentSummary.setLastPaymentDate(lastPayment.map(p -> p.getEndDate().toString()).orElse(null));
        paymentSummary.setRestaurants(restaurantSummaries);

        PaymentSummary.GlobalStats globalStats = new PaymentSummary.GlobalStats();
        globalStats.setTotalAmountWithSubsidy(Math.round(totalWithSubsidy * 100.0) / 100.0);
        globalStats.setTotalAmountWithoutSubsidy(Math.round(totalWithoutSubsidy * 100.0) / 100.0);
        globalStats.setTotalSubsidy(Math.round(totalSubsidy * 100.0) / 100.0);
        globalStats.setTotalDeliveryFees(Math.round(totalDeliveryFees * 100.0) / 100.0);
        globalStats.setTotalOrders(orders.size());
        globalStats.setTotalUsers(uniqueUsers.size());

        paymentSummary.setGlobalStats(globalStats);

        log.info("✅ Récapitulatif généré: {} restaurants, {} commandes, {} utilisateurs",
                restaurantSummaries.size(), orders.size(), uniqueUsers.size());

        return paymentSummary;
    }

    /**
     * 🏪 Calcule le récapitulatif pour un restaurant AVEC frais de livraison
     */
    private RestaurantPaymentSummary calculateRestaurantSummary(
            Restaurant restaurant,
            List<Order> orders,
            LocalDate monday,
            LocalDate friday
    ) {
        Set<Long> uniqueUsers = new HashSet<>();
        double totalWithSubsidy = 0.0;
        double totalWithoutSubsidy = 0.0;
        double totalSubsidy = 0.0;

        for (Order order : orders) {
            uniqueUsers.add(order.getUser().getId());

            // Calculer les montants pour ce restaurant uniquement
            double restaurantSubtotal = order.getOrderItems().stream()
                    .filter(item -> item.getRestaurant().getId().equals(restaurant.getId()))
                    .mapToDouble(OrderItem::getSubtotal)
                    .sum();

            // Proportionner la subvention
            double orderSubtotal = order.getSubtotal();
            double subsidyProportion = orderSubtotal > 0 ? restaurantSubtotal / orderSubtotal : 0;
            double restaurantSubsidy = Math.min(SUBSIDY_AMOUNT * subsidyProportion, restaurantSubtotal);

            totalWithoutSubsidy += restaurantSubtotal;
            totalSubsidy += restaurantSubsidy;
            totalWithSubsidy += Math.max(0, restaurantSubtotal - restaurantSubsidy);
        }

        // 🚚 CALCULER LES FRAIS DE LIVRAISON pour ce restaurant
        double deliveryFees = calculateRestaurantDeliveryFees(restaurant, monday, friday, uniqueUsers.size());

        RestaurantPaymentSummary summary = new RestaurantPaymentSummary();
        summary.setRestaurantId(restaurant.getId());
        summary.setRestaurantName(restaurant.getRestaurantName());
        summary.setTotalAmountWithSubsidy(Math.round(totalWithSubsidy * 100.0) / 100.0);
        summary.setTotalAmountWithoutSubsidy(Math.round(totalWithoutSubsidy * 100.0) / 100.0);
        summary.setSubsidyAmount(Math.round(totalSubsidy * 100.0) / 100.0);
        summary.setDeliveryFees(Math.round(deliveryFees * 100.0) / 100.0);
        summary.setOrderCount(orders.size());
        summary.setUserCount(uniqueUsers.size());

        return summary;
    }

    /**
     * 🚚 Calcule les frais de livraison TOTAUX pour un restaurant sur la semaine
     */
    private double calculateRestaurantDeliveryFees(
            Restaurant restaurant,
            LocalDate monday,
            LocalDate friday,
            int totalUsers
    ) {
        Double dailyFee = restaurant.getDeliveryFee();

        if (dailyFee == null || dailyFee <= 0) {
            return 0.0;
        }

        // Compter combien de jours de la semaine il y a eu des commandes
        List<Order> restaurantOrders = orderRepository.findByOrderDateBetweenAndStatus(
                monday, friday, OrderStatus.CONFIRMED
        );

        Set<LocalDate> daysWithOrders = restaurantOrders.stream()
                .flatMap(order -> order.getOrderItems().stream())
                .filter(item -> item.getRestaurant().getId().equals(restaurant.getId()))
                .map(item -> item.getOrder().getOrderDate())
                .collect(Collectors.toSet());

        // Frais total = frais journalier × nombre de jours avec commandes
        return dailyFee * daysWithOrders.size();
    }

    /**
     * ✅ Valide et enregistre le paiement
     * 🚚 CALCULE AUTOMATIQUEMENT LES FRAIS DE LIVRAISON
     */
    @Override
    @Transactional
    public PaymentValidationResponse validatePayment(PaymentValidationRequest request) {
        log.info("✅ Validation du paiement");

        // ⚠️ VÉRIFICATION: On doit être vendredi
        if (LocalDate.now().getDayOfWeek() != DayOfWeek.FRIDAY) {
            throw new RuntimeException("⚠️ La validation des paiements ne peut être effectuée que le VENDREDI");
        }

        // 📅 Forcer la période à la semaine courante
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate friday = LocalDate.now();

        // 1️⃣ CALCULER LES FRAIS DE LIVRAISON AVANT de générer le récapitulatif
        log.info("🚚 Calcul des frais de livraison...");
        deliveryFeeService.calculateDeliveryFeesForPeriod(monday, friday);

        // 2️⃣ Générer le récapitulatif (maintenant avec frais de livraison inclus)
        PaymentSummary summary = generatePaymentSummary(monday, friday);

        if (summary.getGlobalStats().getTotalOrders() == 0) {
            throw new RuntimeException("Aucune commande à payer cette semaine");
        }

        // 3️⃣ Enregistrer le paiement
        PaymentRecord paymentRecord = new PaymentRecord();
        paymentRecord.setStartDate(monday);
        paymentRecord.setEndDate(friday);
        paymentRecord.setTotalAmountWithSubsidy(summary.getGlobalStats().getTotalAmountWithSubsidy());
        paymentRecord.setTotalAmountWithoutSubsidy(summary.getGlobalStats().getTotalAmountWithoutSubsidy());
        paymentRecord.setTotalSubsidy(summary.getGlobalStats().getTotalSubsidy());
        paymentRecord.setTotalDeliveryFees(summary.getGlobalStats().getTotalDeliveryFees());
        paymentRecord.setOrderCount(summary.getGlobalStats().getTotalOrders());
        paymentRecord.setUserCount(summary.getGlobalStats().getTotalUsers());

        try {
            paymentRecord.setRestaurantDetails(objectMapper.writeValueAsString(summary.getRestaurants()));
        } catch (Exception e) {
            log.error("Erreur lors de la sérialisation des détails restaurant", e);
        }

        paymentRecordRepository.save(paymentRecord);

        log.info("💾 Paiement enregistré avec succès pour la semaine du {} au {}", monday, friday);

        // 4️⃣ Créer la réponse
        PaymentValidationResponse response = new PaymentValidationResponse();
        response.setSuccess(true);
        response.setMessage("Paiement validé avec succès pour la semaine du " + monday + " au " + friday);
        response.setDeliveryFeesCalculated(true);
        response.setAffectedOrders(summary.getGlobalStats().getTotalOrders());
        response.setAffectedUsers(summary.getGlobalStats().getTotalUsers());
        response.setPaymentDate(friday.toString());

        return response;
    }

    /**
     * 📅 Récupère la date du dernier paiement
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<LocalDate> getLastPaymentDate() {
        return paymentRecordRepository.findLastPayment()
                .map(PaymentRecord::getEndDate);
    }

    /**
     * ✅ Vérifie si on peut valider les paiements (vendredi uniquement)
     */
    @Override
    public boolean canValidatePayments() {
        return LocalDate.now().getDayOfWeek() == DayOfWeek.FRIDAY;
    }

    /**
     * 📋 Crée un récapitulatif vide
     */
    private PaymentSummary createEmptyPaymentSummary(
            LocalDate startDate,
            LocalDate endDate,
            Optional<PaymentRecord> lastPayment
    ) {
        PaymentSummary summary = new PaymentSummary();
        summary.setStartDate(startDate.toString());
        summary.setEndDate(endDate.toString());
        summary.setLastPaymentDate(lastPayment.map(p -> p.getEndDate().toString()).orElse(null));
        summary.setRestaurants(new ArrayList<>());

        PaymentSummary.GlobalStats globalStats = new PaymentSummary.GlobalStats();
        globalStats.setTotalAmountWithSubsidy(0.0);
        globalStats.setTotalAmountWithoutSubsidy(0.0);
        globalStats.setTotalSubsidy(0.0);
        globalStats.setTotalDeliveryFees(0.0);
        globalStats.setTotalOrders(0);
        globalStats.setTotalUsers(0);

        summary.setGlobalStats(globalStats);
        return summary;
    }
}
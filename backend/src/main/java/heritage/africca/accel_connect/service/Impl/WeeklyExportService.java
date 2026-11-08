package heritage.africca.accel_connect.service.Impl;

import heritage.africca.accel_connect.dto.WeeklyExportDTO;
import heritage.africca.accel_connect.entity.*;
import heritage.africca.accel_connect.entity.enumeration.OrderStatus;
import heritage.africca.accel_connect.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 📊 SERVICE DE PRÉPARATION DES DONNÉES D'EXPORT HEBDOMADAIRE
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WeeklyExportService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final UserBalanceRepository userBalanceRepository;
    private final RestaurantRepository restaurantRepository;
    private final ExportHistoryService exportHistoryService;


    private static final double SUBSIDY_AMOUNT = 1000.0;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * 📋 Prépare les données pour l'export hebdomadaire
     * ⚠️ Par défaut: semaine en cours (Lundi-Vendredi)
     */
    @Transactional(readOnly = true)
    public WeeklyExportDTO prepareWeeklyExportData(LocalDate startDate, LocalDate endDate) {
        log.info("📊 Préparation des données d'export du {} au {}", startDate, endDate);

        WeeklyExportDTO exportData = new WeeklyExportDTO();
        exportData.setStartDate(startDate.format(DATE_FORMATTER));
        exportData.setEndDate(endDate.format(DATE_FORMATTER));
        exportData.setExportDate(LocalDate.now().format(DATE_FORMATTER));

        // Récupérer TOUTES les commandes de la semaine
        List<Order> weekOrders = orderRepository.findByOrderDateBetweenAndStatus(
                startDate, endDate, OrderStatus.CONFIRMED
        );


        // Récupérer TOUS les utilisateurs qui ont commandé dans la semaine
        Set<Long> userIds = weekOrders.stream()
                .map(order -> order.getUser().getId())
                .collect(Collectors.toSet());

        List<User> users = userRepository.findAllById(userIds);

        // Préparer les consommations par utilisateur
        List<WeeklyExportDTO.UserWeeklyConsumption> userConsumptions = new ArrayList<>();

        for (User user : users) {
            WeeklyExportDTO.UserWeeklyConsumption userConsumption = prepareUserConsumption(
                    user, weekOrders, startDate, endDate
            );
            userConsumptions.add(userConsumption);
        }

        // Trier par nom d'utilisateur
        userConsumptions.sort(Comparator.comparing(WeeklyExportDTO.UserWeeklyConsumption::getUsername));

        exportData.setUserConsumptions(userConsumptions);

        // Calculer les statistiques globales
        WeeklyExportDTO.WeeklyGlobalStats globalStats = calculateGlobalStats(weekOrders, userConsumptions);
        exportData.setGlobalStats(globalStats);

        log.info("✅ Données d'export préparées: {} utilisateurs", userConsumptions.size());

        return exportData;
    }

    /**
     * 👤 Prépare les données de consommation d'un utilisateur
     */
    private WeeklyExportDTO.UserWeeklyConsumption prepareUserConsumption(
            User user,
            List<Order> allOrders,
            LocalDate startDate,
            LocalDate endDate
    ) {
        WeeklyExportDTO.UserWeeklyConsumption consumption = new WeeklyExportDTO.UserWeeklyConsumption();

        consumption.setUserId(user.getId());
        consumption.setUsername(user.getUsername());
        consumption.setEmail(user.getEmail());
        consumption.setFirstName(user.getFirstName());
        consumption.setLastName(user.getLastName());

        // Filtrer les commandes de cet utilisateur
        List<Order> userOrders = allOrders.stream()
                .filter(order -> order.getUser().getId().equals(user.getId()))
                .collect(Collectors.toList());

        // Préparer les consommations par jour
        Map<String, WeeklyExportDTO.DayConsumption> dailyConsumptions = new LinkedHashMap<>();

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
            String dayKey = dayOfWeek.name();

            WeeklyExportDTO.DayConsumption dayConsumption = prepareDayConsumption(
                    currentDate, userOrders
            );

            dailyConsumptions.put(dayKey, dayConsumption);
            currentDate = currentDate.plusDays(1);
        }

        consumption.setDailyConsumptions(dailyConsumptions);

        // Calculer les totaux
        consumption.setTotalOrders(userOrders.size());

        double totalWithSubsidy = userOrders.stream()
                .mapToDouble(Order::getTotalAmount)
                .sum();

        double totalWithoutSubsidy = userOrders.stream()
                .mapToDouble(Order::getSubtotal)
                .sum();

        double totalSubsidy = userOrders.stream()
                .mapToDouble(Order::getSubsidyAmount)
                .sum();

        double totalDeliveryFees = userOrders.stream()
                .mapToDouble(Order::getDeliveryFeeShare)
                .sum();

        consumption.setTotalAmountWithSubsidy(Math.round(totalWithSubsidy * 100.0) / 100.0);
        consumption.setTotalAmountWithoutSubsidy(Math.round(totalWithoutSubsidy * 100.0) / 100.0);
        consumption.setTotalSubsidy(Math.round(totalSubsidy * 100.0) / 100.0);
        consumption.setTotalDeliveryFees(Math.round(totalDeliveryFees * 100.0) / 100.0);

        // Récupérer le solde
        UserBalance balance = userBalanceRepository.findByUserId(user.getId())
                .orElse(new UserBalance());

        consumption.setBalance(balance.getBalance());
        consumption.setHasDebt(balance.hasDebt());

        // Collecter les restaurants
        Set<String> restaurants = userOrders.stream()
                .flatMap(order -> order.getOrderItems().stream())
                .map(item -> item.getRestaurant().getRestaurantName())
                .collect(Collectors.toSet());

        consumption.setRestaurants(new ArrayList<>(restaurants));

        return consumption;
    }

    /**
     * 📅 Prépare la consommation d'une journée
     */
    private WeeklyExportDTO.DayConsumption prepareDayConsumption(
            LocalDate date,
            List<Order> userOrders
    ) {
        WeeklyExportDTO.DayConsumption dayConsumption = new WeeklyExportDTO.DayConsumption();

        dayConsumption.setDate(date.format(DATE_FORMATTER));
        dayConsumption.setDayName(date.getDayOfWeek().getDisplayName(
                TextStyle.FULL, Locale.FRENCH
        ));

        // Trouver la commande du jour
        Optional<Order> dayOrder = userOrders.stream()
                .filter(order -> order.getOrderDate().equals(date))
                .findFirst();

        if (dayOrder.isPresent()) {
            Order order = dayOrder.get();
            dayConsumption.setHasOrder(true);
            dayConsumption.setDailyTotal(order.getTotalAmount());

            // Préparer les détails des plats
            List<WeeklyExportDTO.MealDetail> meals = order.getOrderItems().stream()
                    .map(item -> {
                        WeeklyExportDTO.MealDetail meal = new WeeklyExportDTO.MealDetail();
                        meal.setMealName(item.getMeal().getMealName());
                        meal.setRestaurantName(item.getRestaurant().getRestaurantName());
                        meal.setQuantity(item.getQuantity());
                        meal.setUnitPrice(item.getUnitPrice());
                        meal.setSubtotal(item.getSubtotal());
                        return meal;
                    })
                    .collect(Collectors.toList());

            dayConsumption.setMeals(meals);
        } else {
            dayConsumption.setHasOrder(false);
            dayConsumption.setDailyTotal(0.0);
            dayConsumption.setMeals(new ArrayList<>());
        }

        return dayConsumption;
    }

    /**
     * 📊 Calcule les statistiques globales
     */
    private WeeklyExportDTO.WeeklyGlobalStats calculateGlobalStats(
            List<Order> orders,
            List<WeeklyExportDTO.UserWeeklyConsumption> userConsumptions
    ) {
        WeeklyExportDTO.WeeklyGlobalStats stats = new WeeklyExportDTO.WeeklyGlobalStats();

        stats.setTotalUsers(userConsumptions.size());
        stats.setTotalOrders(orders.size());

        double totalWithSubsidy = orders.stream()
                .mapToDouble(Order::getTotalAmount)
                .sum();

        double totalWithoutSubsidy = orders.stream()
                .mapToDouble(Order::getSubtotal)
                .sum();

        double totalSubsidy = orders.stream()
                .mapToDouble(Order::getSubsidyAmount)
                .sum();

        double totalDeliveryFees = orders.stream()
                .mapToDouble(Order::getDeliveryFeeShare)
                .sum();

        stats.setTotalAmountWithSubsidy(Math.round(totalWithSubsidy * 100.0) / 100.0);
        stats.setTotalAmountWithoutSubsidy(Math.round(totalWithoutSubsidy * 100.0) / 100.0);
        stats.setTotalSubsidy(Math.round(totalSubsidy * 100.0) / 100.0);
        stats.setTotalDeliveryFees(Math.round(totalDeliveryFees * 100.0) / 100.0);

        // Compter les restaurants uniques
        Set<Long> restaurantIds = orders.stream()
                .flatMap(order -> order.getOrderItems().stream())
                .map(item -> item.getRestaurant().getId())
                .collect(Collectors.toSet());

        stats.setTotalRestaurants(restaurantIds.size());

        return stats;
    }

    /**
     * 📅 Retourne la semaine en cours (Lundi-Vendredi)
     */
    public LocalDate[] getCurrentWeek() {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate friday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));
        return new LocalDate[]{monday, friday};
    }

    /**
     * 📋 RÉCUPÈRE TOUTES LES SEMAINES DISPONIBLES POUR EXPORT
     */
    // 📋 CORRECTION À APPLIQUER DANS WeeklyExportService.java
// Remplacer la section autour de la ligne 310-330

    /**
     * 📋 RÉCUPÈRE TOUTES LES SEMAINES DISPONIBLES POUR EXPORT
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAvailableWeeks() {
        log.info("📋 Recherche des semaines disponibles...");

        // Récupérer toutes les commandes confirmées
        List<Order> allOrders = orderRepository.findByStatus(OrderStatus.CONFIRMED);

        if (allOrders.isEmpty()) {
            log.info("ℹ️ Aucune commande confirmée trouvée");
            return Collections.emptyList();
        }

        // Grouper les commandes par semaine
        Map<String, WeekData> weekDataMap = new TreeMap<>(Comparator.reverseOrder());

        for (Order order : allOrders) {
            LocalDate orderDate = order.getOrderDate();
            LocalDate monday = orderDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate friday = monday.plusDays(4);

            String weekKey = monday.toString();

            WeekData weekData = weekDataMap.computeIfAbsent(weekKey, k -> {
                WeekData wd = new WeekData();
                wd.startDate = monday;
                wd.endDate = friday;
                wd.weekNumber = monday.get(WeekFields.ISO.weekOfWeekBasedYear());
                wd.year = monday.get(WeekFields.ISO.weekBasedYear());
                return wd;
            });

            weekData.orders.add(order);
            weekData.totalOrders++;
            if (order.getStatus() == OrderStatus.CONFIRMED) {
                weekData.totalAmount += order.getTotalAmount();
            }
        }

        // Convertir en liste de maps
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map.Entry<String, WeekData> entry : weekDataMap.entrySet()) {
            WeekData wd = entry.getValue();

            // Vérifier si cette semaine a déjà été exportée
            boolean hasBeenDownloaded = exportHistoryService.hasBeenExported(
                    wd.startDate, wd.endDate
            );

            Optional<LocalDate> downloadDate = Optional.empty();

            if (hasBeenDownloaded) {
                List<ExportHistory> exports = exportHistoryService.getExportsByPeriod(wd.startDate, wd.endDate);

                // Filtrer les exports avec exportDate non null et prendre le plus récent
                downloadDate = exports.stream()
                        .filter(h -> h.getExportDate() != null)  // ✅ Filtrer les NULL
                        .max(Comparator.comparing(ExportHistory::getExportDate))
                        .map(h -> h.getExportDate().toLocalDate());
            }

            Map<String, Object> weekInfo = new HashMap<>();
            weekInfo.put("weekNumber", wd.weekNumber);
            weekInfo.put("year", wd.year);
            weekInfo.put("startDate", wd.startDate.toString());
            weekInfo.put("endDate", wd.endDate.toString());
            weekInfo.put("label", String.format("Semaine %d - %s", wd.weekNumber, wd.year));
            weekInfo.put("totalOrders", wd.totalOrders);
            weekInfo.put("totalAmount", Math.round(wd.totalAmount * 100.0) / 100.0);
            weekInfo.put("hasBeenDownloaded", hasBeenDownloaded);
            downloadDate.ifPresent(date -> weekInfo.put("downloadDate", date.toString()));

            result.add(weekInfo);
        }

        log.info("✅ {} semaine(s) disponible(s) trouvée(s)", result.size());
        return result;
    }

    /**
     * 📋 Crée un export vide
     */
    private WeeklyExportDTO createEmptyExportData(LocalDate startDate, LocalDate endDate) {
        WeeklyExportDTO dto = new WeeklyExportDTO();
        dto.setStartDate(startDate.toString());
        dto.setEndDate(endDate.toString());
        dto.setExportDate(LocalDate.now().format(DATE_FORMATTER));
        dto.setUserConsumptions(new ArrayList<>());

        WeeklyExportDTO.WeeklyGlobalStats globalStats = new WeeklyExportDTO.WeeklyGlobalStats();
        globalStats.setTotalOrders(0);
        globalStats.setTotalUsers(0);
        globalStats.setTotalRestaurants(0);
        globalStats.setTotalAmountWithSubsidy(0.0);
        globalStats.setTotalAmountWithoutSubsidy(0.0);
        globalStats.setTotalSubsidy(0.0);
        globalStats.setTotalDeliveryFees(0.0);

        dto.setGlobalStats(globalStats);
        return dto;
    }

    // === CLASSES HELPER ===

    private static class WeekData {
        LocalDate startDate;
        LocalDate endDate;
        int weekNumber;
        int year;
        int totalOrders = 0;
        double totalAmount = 0.0;
        List<Order> orders = new ArrayList<>();
    }

    private static class UserOrdersData {
        Long userId;
        String username;
        String email;
        double balance;
        boolean hasDebt;
        List<Order> orders = new ArrayList<>();
    }
}
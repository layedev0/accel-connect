package heritage.africca.accel_connect.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 📊 DTO pour l'export Excel hebdomadaire
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyExportDTO {

    private String startDate;
    private String endDate;
    private String exportDate;
    private List<UserWeeklyConsumption> userConsumptions;
    private WeeklyGlobalStats globalStats;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserWeeklyConsumption {
        private Long userId;
        private String username;
        private String email;
        private String firstName;
        private String lastName;

        // Détails par jour
        private Map<String, DayConsumption> dailyConsumptions; // Key: "Lundi", "Mardi", etc.

        // Totaux
        private int totalOrders;
        private double totalAmountWithSubsidy;
        private double totalAmountWithoutSubsidy;
        private double totalSubsidy;
        private double totalDeliveryFees;
        private double balance;
        private boolean hasDebt;

        // Restaurants commandés
        private List<String> restaurants;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayConsumption {
        private String date;
        private String dayName; // "Lundi", "Mardi", etc.
        private List<MealDetail> meals;
        private double dailyTotal;
        private boolean hasOrder;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MealDetail {
        private String mealName;
        private String restaurantName;
        private int quantity;
        private double unitPrice;
        private double subtotal;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeeklyGlobalStats {
        private int totalUsers;
        private int totalOrders;
        private double totalAmountWithSubsidy;
        private double totalAmountWithoutSubsidy;
        private double totalSubsidy;
        private double totalDeliveryFees;
        private int totalRestaurants;
    }
}
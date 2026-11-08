package heritage.africca.accel_connect.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSummary {
    private String startDate;
    private String endDate;
    private String lastPaymentDate;
    private List<RestaurantPaymentSummary> restaurants;
    private GlobalStats globalStats;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GlobalStats {
        private Double totalAmountWithSubsidy;
        private Double totalAmountWithoutSubsidy;
        private Double totalSubsidy;
        private Double totalDeliveryFees;
        private Integer totalOrders;
        private Integer totalUsers;
    }
}
package heritage.africca.accel_connect.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TodayOrderStatsDTO {
    private LocalDate date;
    private Integer totalOrders;
    private Double totalAmount;
    private Integer confirmedCount;
    private Integer cancelledCount;
    private List<RestaurantOrderStatsDTO> restaurantStats;
}

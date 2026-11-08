package heritage.africca.accel_connect.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RestaurantOrderStatsDTO {
    private Long restaurantId;
    private String restaurantName;
    private Integer orderCount;
    private Double totalAmount;
    private Double deliveryFee;
    private List<OrderDTO> orders;
}

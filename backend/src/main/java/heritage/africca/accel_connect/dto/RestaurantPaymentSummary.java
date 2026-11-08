package heritage.africca.accel_connect.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantPaymentSummary {
    private Long restaurantId;
    private String restaurantName;
    private Double totalAmountWithSubsidy;
    private Double totalAmountWithoutSubsidy;
    private Double subsidyAmount;
    private Double deliveryFees;
    private Integer orderCount;
    private Integer userCount;
}
package heritage.africca.accel_connect.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantDTO {
    private Long id;
    private String restaurantName;
    private String address;
    private String contact;
    private Double deliveryFee;
    private Boolean ordersBlocked;
    private String blockMessage;
    private List<MealDTO> meals;
}
package heritage.africca.accel_connect.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemData {
    private Long mealId;
    private Long restaurantId;
    private Integer quantity;
    private Double unitPrice;
}
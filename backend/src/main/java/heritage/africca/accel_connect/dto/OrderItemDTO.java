package heritage.africca.accel_connect.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDTO {
    private Long id;
    private Long mealId;
    private String mealName;
    private Long restaurantId;
    private String restaurantName;
    private Integer quantity;
    private Double unitPrice;
    private Double subtotal;
}
package heritage.africca.accel_connect.dto;

import lombok.Data;

import java.util.List;

@Data
public class OrderData {
    private Long restaurantId;
    private List<OrderItemData> items;
    private Double subtotal;
    private Double subsidyAmount;
    private Double amountAfterSubsidy;
}

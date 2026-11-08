package heritage.africca.accel_connect.dto;

import heritage.africca.accel_connect.entity.enumeration.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    private Long id;
    private Long userId;
    private String username;
    private LocalDate orderDate;

    // ✅ AJOUT
    private String restaurantName;

    private Double subtotal;
    private Double subsidyAmount;
    private Double amountAfterSubsidy;
    private Double deliveryFeeShare;
    private Double totalAmount;
    private String status;
    private LocalDateTime createdAt;
    private List<OrderItemDTO> items;
}
package heritage.africca.accel_connect.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminCreateOrderRequest {
    private Long userId;
    private LocalDate date;
    private List<OrderItemData> items;
}

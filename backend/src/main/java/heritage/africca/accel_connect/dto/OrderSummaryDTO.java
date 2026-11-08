package heritage.africca.accel_connect.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderSummaryDTO {
    private Double subtotal;
    private Double subsidyAmount = 1000.0;
    private Double amountAfterSubsidy;
    private String message;
    private Boolean canOrder;  // Peut commander même si en dette
}
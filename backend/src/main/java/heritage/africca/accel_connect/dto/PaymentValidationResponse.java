package heritage.africca.accel_connect.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentValidationResponse {
    private Boolean success;
    private String message;
    private Boolean deliveryFeesCalculated;
    private Integer affectedOrders;
    private Integer affectedUsers;
    private String paymentDate;
}

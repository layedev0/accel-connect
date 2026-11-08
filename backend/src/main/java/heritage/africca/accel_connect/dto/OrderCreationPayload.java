package heritage.africca.accel_connect.dto;

import heritage.africca.accel_connect.entity.enumeration.OrderMode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreationPayload {
    private List<OrderItemData> items;

    /**
     * 🛒 Mode de prise de commande. Null est traité comme {@link OrderMode#ADD}
     * pour rester compatible avec les clients qui n'envoient pas ce champ.
     */
    private OrderMode mode;
}
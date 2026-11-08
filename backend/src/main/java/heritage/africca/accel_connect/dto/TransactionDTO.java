package heritage.africca.accel_connect.dto;

import heritage.africca.accel_connect.entity.enumeration.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionDTO {
    private Long id;
    private Long walletId;
    private Long orderId;
    private String orderReference;
    private TransactionType type;
    private Double amount;
    private Double balanceBefore;
    private Double balanceAfter;
    private String description;
    private String createdByUsername;
    private LocalDateTime createdAt;
}
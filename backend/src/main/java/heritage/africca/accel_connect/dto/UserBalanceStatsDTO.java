package heritage.africca.accel_connect.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserBalanceStatsDTO {
    private Long userId;
    private String username;
    private String email;
    private Double totalDeposited;
    private Double totalSpent;
    private Double balance;
    private Integer orderCount;
    private String status;
    private Boolean hasDebt;
    private Double debtAmount;
    private Double refundableAmount;
    private Double estimatedDeliveryFees; // Frais de livraison estimés pour la semaine en cours
}

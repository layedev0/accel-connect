package heritage.africca.accel_connect.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserWalletStatsDTO {
    private Long userId;
    private String username;
    private String email;
    private Double balance;
    private Double totalDeposited;
    private Double totalSpent;
    private Integer totalOrders;
    private String status;
}
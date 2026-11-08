package heritage.africca.accel_connect.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WalletDTO {
    private Long id;
    private Long userId;
    private String username;
    private Double balance;
    private Double totalDeposited;
    private Double totalSpent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
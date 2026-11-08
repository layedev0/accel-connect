package heritage.africca.accel_connect.dto;

import lombok.Data;

@Data
public class UpdateBalanceRequest {
    private Long userId;
    private Double newBalance;
    private String reason;
}
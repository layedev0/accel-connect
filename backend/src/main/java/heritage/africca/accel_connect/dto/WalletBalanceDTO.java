package heritage.africca.accel_connect.dto;

import lombok.Data;

@Data
public class WalletBalanceDTO {
    private Double balance;
    private Double creditBalance;
    private Double debitBalance;
    private Double totalDeposited;
    private Double totalSpent;
    private String message;
    private Boolean hasCredit;
    private Double creditAmount;
}
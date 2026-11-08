package heritage.africca.accel_connect.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserBalanceDTO {
    private Long userId;
    private String username;
    private Double totalDeposited;
    private Double totalSpent;
    private Double balance;
    private Boolean hasDebt;
    private Double debtAmount;
    private Double refundableAmount;
    private String message;
    private Double estimatedDeliveryFees;

    // Dernier dépôt
    private Double lastDepositAmount;
    private LocalDateTime lastDepositDate;

    // Historique des 4 derniers dépôts
    private List<DepositHistoryDTO> recentDeposits;

    // Dépenses semaine en cours
    private Double currentWeekSpent;

    // Dépenses des 4 dernières semaines
    private List<WeeklySpendingDTO> recentWeeksSpending;

    // Avoir en caisse (balance positive ou 0)
    private Double avoir;

    // Dette (balance négative en valeur absolue, ou 0)
    private Double debt;
}

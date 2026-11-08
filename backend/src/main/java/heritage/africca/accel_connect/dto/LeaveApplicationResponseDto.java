package heritage.africca.accel_connect.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveApplicationResponseDto {

    private Integer leaveRequestId;
    private String status;
    private String message;
    /** Total working days in the requested range. */
    private BigDecimal daysRequested;
    /** Days that will actually be deducted from the balance (after applying leave-type rules). */
    private BigDecimal balanceDeductibleDays;
    private BigDecimal availableBalance;
    private BigDecimal balanceAfter;
    private List<LeaveDayDetailDto> leaveDays;
    private List<String> warnings;
}

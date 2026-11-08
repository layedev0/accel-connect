package heritage.africca.accel_connect.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveBalanceInfoDto {

    private String leaveTypeCode;
    private String leaveTypeName;
    private BigDecimal entitled;
    private BigDecimal taken;
    private BigDecimal scheduled;
    private BigDecimal pending;
    private BigDecimal available;
    private String displayValue;
}

package heritage.africca.accel_connect.dto;

import heritage.africca.accel_connect.entity.enumeration.LeaveTypeCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveTypeDto {

    private String code;
    private String name;
    private int allocatedDays;
    private String deductionRule;

    public static LeaveTypeDto from(LeaveTypeCode ltc) {
        return new LeaveTypeDto(
                ltc.getCode(),
                ltc.getDisplayName(),
                ltc.getAllocatedDays(),
                ltc.getDeductionRule().name()
        );
    }
}

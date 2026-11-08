package heritage.africca.accel_connect.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveDecisionRequest {

    private String comment;
    private Long decidedByUserId;
}

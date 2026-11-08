package heritage.africca.accel_connect.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLeaveBalanceDto {

    /** User ID of the employee. */
    private Long userId;
    /** User's username   */
    private String userName;
    /** User's email address. */
    private String userEmail;
    /** Total number of leave days allocated to the employee for the current year. */
    private BigDecimal balanceDays;
    /** Date when the leave balance was last updated. */
    private LocalDate lastUpdated;

    /** Total days ever allocated to the employee (leaveBalance). */
    private BigDecimal leaveDue;

    /** Total approved/taken leave days (SCHEDULED ). */
    private BigDecimal leaveTaken;

    /** Remaining balance (= balanceDays = leaveDue - leaveTaken). */
    private BigDecimal leaveBalance;
}

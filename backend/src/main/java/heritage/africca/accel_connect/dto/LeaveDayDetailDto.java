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
public class LeaveDayDetailDto {

    private LocalDate date;
    private String dayOfWeek;
    private BigDecimal lengthDays;
    private BigDecimal lengthHours;
    private String status;
    private String durationType;
    private boolean isWeekend;
    private boolean isHoliday;
}

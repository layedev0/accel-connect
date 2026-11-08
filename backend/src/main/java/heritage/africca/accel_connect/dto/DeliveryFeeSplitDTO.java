package heritage.africca.accel_connect.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeliveryFeeSplitDTO {
    private Long id;
    private String username;
    private String restaurantName;
    private LocalDate weekStartDate;
    private Integer orderCount;
    private Integer totalUsersCount;
    private Double restaurantWeeklyFee;
    private Double userShareAmount;
    private LocalDateTime calculatedAt;
}

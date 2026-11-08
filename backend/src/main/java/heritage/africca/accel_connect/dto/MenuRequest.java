package heritage.africca.accel_connect.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
class MenuRequest {
    private Long restaurantId;
    private LocalDate menuDate;
    private List<Long> mealIds;
}

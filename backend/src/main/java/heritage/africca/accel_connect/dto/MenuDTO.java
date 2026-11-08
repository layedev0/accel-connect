package heritage.africca.accel_connect.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MenuDTO {
    private Long id;
    private Long restaurantId;
    private String restaurantName;
    private LocalDate menuDate;
    private List<Long> mealIds;
    private List<MealDTO> meals;
    private Boolean isActive;
}
package heritage.africca.accel_connect.dto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MealDTO {
    private Long id;
    private String mealName;
    private Double unitPrice;
    private String imageUrl;
}

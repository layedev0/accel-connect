package heritage.africca.accel_connect.dto;

import heritage.africca.accel_connect.entity.DateAudit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobCategoryRequest extends DateAudit {

    @NotBlank(message = "Name is required")
    @Size(max = 60)
    private String name;

    @Size(max = 255)
    private String description;

    @Size(max = 3)
    @NotBlank(message = "Code must not be blank")
    private String code;

    @NotBlank(message = "Category code is required")
    @Size(max = 10)
    private String categoryCode;

    private BigDecimal salaireBase;
}

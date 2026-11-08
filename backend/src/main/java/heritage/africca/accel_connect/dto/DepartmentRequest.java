package heritage.africca.accel_connect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 60)
    private String name;

    @Size(max = 255)
    private String description;

    @NotBlank(message = "Code must not be blank")
    @Size(max = 3)
    private String code;
}

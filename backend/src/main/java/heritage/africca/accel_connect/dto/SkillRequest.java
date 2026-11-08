package heritage.africca.accel_connect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkillRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 120)
    private String name;

    @Size(max = 1000)
    private String description;
}

package heritage.africca.accel_connect.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmployeeSkillDto {
    private Long employeeId;

    @NotNull(message = "Skill ID is required")
    private Integer skillId;

    private String skillName; // For display purposes

    @Min(value = 0, message = "Years of experience must be at least 0")
    @Max(value = 50, message = "Years of experience must not exceed 50")
    private Integer yearsOfExperience;

    @Size(max = 100, message = "Comments must not exceed 100 characters")
    private String comments;
}
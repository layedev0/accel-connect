package heritage.africca.accel_connect.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmployeeDependentDto {
    private Long employeeId;
    private Integer seqNo;

    @NotBlank(message = "Dependent name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @NotBlank(message = "Relationship is required")
    @Size(max = 100, message = "Relationship must not exceed 100 characters")
    private String relationship;

    private LocalDate dateOfBirth;

    @Pattern(regexp = "^[0-9+\\-()\\s]*$", message = "Invalid contact number format")
    @Size(max = 20, message = "Contact number must not exceed 20 characters")
    private String contactNumber;


}
package heritage.africca.accel_connect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobTitleRequest {

    @NotBlank(message = "Job title is required")
    @Size(max = 60)
    private String jobTitle;

    @Size(max = 255)
    private String jobDescription;

    @Size(max = 3)
    @NotBlank(message = "Code must not be blank")
    private String jobTitleCode;
}

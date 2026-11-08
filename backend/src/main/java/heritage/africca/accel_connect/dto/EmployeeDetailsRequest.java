package heritage.africca.accel_connect.dto;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDetailsRequest {

    private LocalDate birthday;
    private String sexe;
    private String maritalStatus;
    private String cni;

    private Long jobTitleId;
    private Long jobCategoryId;
    private Long departmentId;

    @Pattern(
            regexp = "^(\\+221)?\\d{9}$",
            message = "Work telephone must be a valid Senegal number: 9 digits (e.g. 771234567) or +221 followed by 9 digits (e.g. +221771234567)"
    )
    private String workTelephone;
    private LocalDate joinedDate;
    private String typeContrat;

    /** Optional: link to existing user account. */
    private Long userId;

    /** Optional: when provided and employee exists, performs update; otherwise creates new. */
    private Long id;
}

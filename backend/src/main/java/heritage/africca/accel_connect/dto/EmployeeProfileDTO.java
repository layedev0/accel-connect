package heritage.africca.accel_connect.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for employee profile display in the RH employee-profile section.
 * Combines User and EmployeeDetails (when available).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmployeeProfileDTO {

    private Long id;
    private Long userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String employeeNumber;
    private String jobTitle;
    private String department;
    private String locationLabel;
    private String status;
    private String phone;
    private Boolean intern;
    private Integer subsidyAmount;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate joinDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;

    private String cni;
    private String sexe;
    private String maritalStatus;
}

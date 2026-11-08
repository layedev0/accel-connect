package heritage.africca.accel_connect.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * A single upcoming employee birthday, derived from the day/month of birth
 * stored on {@code EmployeeDetails} (independent of the birth year).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpcomingBirthdayDTO {

    private Long userId;
    private String firstName;
    private String lastName;
    private String departmentName;


    private Integer day;

    private Integer month;


    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate nextBirthday;


    private int daysUntil;


    private boolean today;

    private Integer turningAge;
}

package heritage.africca.accel_connect.outils;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeSkillId implements Serializable {
    private Long employee;
    private Integer skillId;
}
package heritage.africca.accel_connect.outils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;


@Data
@NoArgsConstructor
@AllArgsConstructor

public class EmployeeDependentId implements Serializable {
    private Long employee;
    private Integer seqNo;
}

package heritage.africca.accel_connect.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import heritage.africca.accel_connect.outils.EmployeeDependentId;
import jakarta.persistence.*;
import jakarta.validation.constraints.Past;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "employee_dependents")
@IdClass(EmployeeDependentId.class)
public class EmployeeDependent {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_number", referencedColumnName = "id")
    private EmployeeDetails employee;

    @Id
    @Column(name = "seqno")
    private Integer seqNo;

    @Column(name = "name", nullable = false,unique = true)
    private String name;
    @Column(name = "ed_relationship", nullable = false)
    private String relationship;
    @Column(name = "contact_number", nullable = false)
    private String contactNumber;

    @Past(message = "Date of birth must be in the past")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

}

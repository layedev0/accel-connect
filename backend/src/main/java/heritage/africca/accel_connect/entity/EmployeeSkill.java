package heritage.africca.accel_connect.entity;

import heritage.africca.accel_connect.outils.EmployeeSkillId;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "employee_skills")
@Data
@NoArgsConstructor
@IdClass(EmployeeSkillId.class)
public class EmployeeSkill {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_number", referencedColumnName = "id")
    private EmployeeDetails employee;

    @Id
    @Column(name = "skill_id")
    private Integer skillId;

    @Column(name = "years_of_exp")
    private Integer yearsOfExperience;

    @Column(name = "comments", length = 100)
    private String comments;
}






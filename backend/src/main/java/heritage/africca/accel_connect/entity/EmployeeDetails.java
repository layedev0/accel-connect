package heritage.africca.accel_connect.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Table(name = "employee_details")
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDetails {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDate birthday;
    private String sexe;
    private String maritalStatus;
    @Column(unique = true)
    private String cni;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_title_id")
    private JobTitle jobTitle;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_category_id")
    private JobCategory jobCategory;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;
    private String workTelephone;
    private LocalDate joinedDate;
    private String typeContrat;

    @Column(name = "day_birth")
    private Integer dayBirth;
    @Column(name = "month_birth")
    private Integer monthBirth;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;
}

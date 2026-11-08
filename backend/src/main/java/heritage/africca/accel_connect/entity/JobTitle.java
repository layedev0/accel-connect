package heritage.africca.accel_connect.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "job_titles")
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobTitle extends DateAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_title", length = 60, nullable = false, unique = true)
    private String jobTitle;

    @Column(name = "job_description", length = 255)
    private String jobDescription;

    @Column(name = "job_title_code", length = 3, unique = true, nullable = false)
    private String jobTitleCode;

    @JsonIgnore
    @OneToMany(mappedBy = "jobTitle", fetch = FetchType.LAZY)
    private List<EmployeeDetails> employees;
}

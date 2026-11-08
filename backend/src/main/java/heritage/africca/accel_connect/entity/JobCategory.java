package heritage.africca.accel_connect.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "job_categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "name", length = 60, nullable = false,unique = true)
    private String name;
    @Column(name = "category_code", length = 10, unique = true, nullable = false)
    private String categoryCode;
    @Column(name = "salaire_base", precision = 10, scale = 2)
    private BigDecimal salaireBase;

    @Column(name = "description", length = 255)
    private String description;
    @Column(name = "code", length = 3, unique = true, nullable = false)
    private String code;
    @JsonIgnore
    @OneToMany(mappedBy = "jobCategory")
    private List<EmployeeDetails> employees;


}
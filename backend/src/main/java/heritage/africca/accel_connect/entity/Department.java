package heritage.africca.accel_connect.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "departments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 60, nullable = false, unique = true)
    private String name;

    @Column(name = "code", length = 3, unique = true, nullable = false)
    private String code;

    @Column(name = "description", length = 255)
    private String description;

    @JsonIgnore
    @OneToMany(mappedBy = "department")
    private List<EmployeeDetails> employees;

}

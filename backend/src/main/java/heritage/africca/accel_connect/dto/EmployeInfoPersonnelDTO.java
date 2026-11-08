package heritage.africca.accel_connect.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeInfoPersonnelDTO {

    private Long id;
    private Long userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String cni;
    private String statut;
    private String telephone;
    private LocalDate dateNaissance;
    private LocalDate dateAdhesion;
    private String sexe;
    private String maritalStatus;
    private Long departmentId;
    private String departmentName;
    private Long jobTitleId;
    private String jobTitleName;
    private Long jobCategoryId;
    private String jobCategoryName;
    private String jobCategoryCode;
    private BigDecimal jobCategorySalaireBase;
}

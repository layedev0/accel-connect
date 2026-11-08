package heritage.africca.accel_connect.repository;

import heritage.africca.accel_connect.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    @Query("SELECT COUNT(e) > 0 FROM EmployeeDetails e WHERE e.department.id = :departmentId")
    boolean hasEmployees(@Param("departmentId") Long departmentId);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}

package heritage.africca.accel_connect.repository;

import heritage.africca.accel_connect.entity.EmployeeSkill;
import heritage.africca.accel_connect.outils.EmployeeSkillId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeSkillRepository extends JpaRepository<EmployeeSkill, EmployeeSkillId> {

    List<EmployeeSkill> findByEmployee_Id(Long employeeId);

    boolean existsByEmployee_IdAndSkillId(Long employeeId, Integer skillId);

    Optional<EmployeeSkill> findByEmployee_IdAndSkillId(Long employeeId, Integer skillId);

    void deleteByEmployee_IdAndSkillId(Long employeeId, Integer skillId);
}


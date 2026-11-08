package heritage.africca.accel_connect.repository;

import heritage.africca.accel_connect.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Integer> {

    @Query("SELECT COUNT(es) > 0 FROM EmployeeSkill es WHERE es.skillId = :skillId")
    boolean hasEmployeeSkills(@Param("skillId") Integer skillId);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Integer id);
}

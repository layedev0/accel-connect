package heritage.africca.accel_connect.repository;

import heritage.africca.accel_connect.entity.JobTitle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JobTitleRepository extends JpaRepository<JobTitle, Long> {

    @Query("SELECT COUNT(e) > 0 FROM EmployeeDetails e WHERE e.jobTitle.id = :jobTitleId")
    boolean hasEmployees(@Param("jobTitleId") Long jobTitleId);

    boolean existsByJobTitleCode(String jobTitleCode);

    boolean existsByJobTitleCodeAndIdNot(String jobTitleCode, Long id);

    boolean existsByJobTitle(String jobTitle);

    boolean existsByJobTitleAndIdNot(String jobTitle, Long id);
}

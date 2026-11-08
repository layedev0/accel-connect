package heritage.africca.accel_connect.repository;

import heritage.africca.accel_connect.entity.JobCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JobCategoryRepository extends JpaRepository<JobCategory, Long> {

    @Query("SELECT COUNT(e) > 0 FROM EmployeeDetails e WHERE e.jobCategory.id = :jobCategoryId")
    boolean hasEmployees(@Param("jobCategoryId") Long jobCategoryId);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    boolean existsByCategoryCode(String categoryCode);

    boolean existsByCategoryCodeAndIdNot(String categoryCode, Long id);
}

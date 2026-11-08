package heritage.africca.accel_connect.repository;

import heritage.africca.accel_connect.entity.EmployeeDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeDetailsRepository extends JpaRepository<EmployeeDetails, Long> {

    @Query("SELECT e FROM EmployeeDetails e " +
            "LEFT JOIN FETCH e.jobTitle " +
            "LEFT JOIN FETCH e.jobCategory " +
            "LEFT JOIN FETCH e.department " +
            "LEFT JOIN FETCH e.user " +
            "WHERE e.id = :id")
    Optional<EmployeeDetails> findByIdWithAssociations(@Param("id") Long id);

    @Query("SELECT e FROM EmployeeDetails e " +
            "LEFT JOIN FETCH e.user " +
            "LEFT JOIN FETCH e.department " +
            "WHERE e.user.id = :userId")
    Optional<EmployeeDetails> findByUserId(@Param("userId") Long userId);

    @Query("SELECT e FROM EmployeeDetails e " +
            "LEFT JOIN FETCH e.jobTitle " +
            "LEFT JOIN FETCH e.department " +
            "LEFT JOIN FETCH e.user " +
            "WHERE e.user.id = :userId")
    Optional<EmployeeDetails> findByUserIdWithAssociations(@Param("userId") Long userId);


    @Query("SELECT e FROM EmployeeDetails e " +
            "LEFT JOIN FETCH e.user " +
            "LEFT JOIN FETCH e.department " +
            "WHERE e.dayBirth IS NOT NULL AND e.monthBirth IS NOT NULL")
    List<EmployeeDetails> findBirthdayCandidates();
}

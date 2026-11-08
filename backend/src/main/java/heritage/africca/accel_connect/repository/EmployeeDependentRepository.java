package heritage.africca.accel_connect.repository;

import heritage.africca.accel_connect.entity.EmployeeDependent;
import heritage.africca.accel_connect.outils.EmployeeDependentId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeDependentRepository extends JpaRepository<EmployeeDependent, EmployeeDependentId> {

    List<EmployeeDependent> findByEmployeeId(Long employeeId);

    Page<EmployeeDependent> findByEmployeeId(Long employeeId, Pageable pageable);

    Optional<EmployeeDependent> findByEmployeeIdAndSeqNo(Long employeeId, Integer seqNo);

    @Query("SELECT MAX(d.seqNo) FROM EmployeeDependent d WHERE d.employee.id = :employeeId")
    Optional<Integer> findMaxSeqNoByEmployeeId(@Param("employeeId") Long employeeId);

    @Query("SELECT d FROM EmployeeDependent d WHERE d.employee.id = :employeeId ORDER BY d.seqNo ASC")
    List<EmployeeDependent> findAllByEmployeeIdOrderBySeqNo(@Param("employeeId") Long employeeId);

    @Query("SELECT COUNT(d) FROM EmployeeDependent d WHERE d.employee.id = :employeeId")
    long countByEmployeeId(@Param("employeeId") Long employeeId);

    boolean existsByEmployeeIdAndNameIgnoreCase(Long employeeId, String name);


    @Query("SELECT d FROM EmployeeDependent d WHERE d.employee.id = :employeeId AND " +
            "(LOWER(d.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(d.relationship) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<EmployeeDependent> searchDependents(@Param("employeeId") Long employeeId,
                                             @Param("searchTerm") String searchTerm);

    @Modifying
    @Transactional
    @Query("DELETE FROM EmployeeDependent d WHERE d.employee.id = :employeeId")
    void deleteAllByEmployeeId(@Param("employeeId") Long employeeId);

    @Modifying
    @Transactional
    @Query("DELETE FROM EmployeeDependent d WHERE d.employee.id = :employeeId AND d.seqNo = :seqNo")
    void deleteByEmployeeIdAndSeqNo(@Param("employeeId") Long employeeId, @Param("seqNo") Integer seqNo);
}
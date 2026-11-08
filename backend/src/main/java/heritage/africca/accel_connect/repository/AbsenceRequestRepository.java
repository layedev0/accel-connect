package heritage.africca.accel_connect.repository;

import heritage.africca.accel_connect.entity.AbsenceRequest;
import heritage.africca.accel_connect.entity.enumeration.AbsenceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AbsenceRequestRepository extends JpaRepository<AbsenceRequest, Integer> {

    @Query("SELECT DISTINCT ar FROM AbsenceRequest ar " +
            "LEFT JOIN FETCH ar.user " +
            "LEFT JOIN FETCH ar.comments c " +
            "LEFT JOIN FETCH c.createdBy " +
            "LEFT JOIN FETCH ar.approvers " +
            "LEFT JOIN FETCH ar.decidedBy " +
            "WHERE ar.id = :id")
    Optional<AbsenceRequest> findByIdWithAssociations(@Param("id") Integer id);

    @Query("SELECT DISTINCT ar FROM AbsenceRequest ar " +
            "LEFT JOIN FETCH ar.comments c " +
            "LEFT JOIN FETCH c.createdBy " +
            "LEFT JOIN FETCH ar.approvers " +
            "LEFT JOIN FETCH ar.decidedBy " +
            "WHERE ar.user.id = :userId")
    List<AbsenceRequest> findByUserIdWithDetails(@Param("userId") Long userId);

    @Query("SELECT DISTINCT ar FROM AbsenceRequest ar " +
            "LEFT JOIN FETCH ar.user " +
            "LEFT JOIN FETCH ar.comments c " +
            "LEFT JOIN FETCH c.createdBy " +
            "LEFT JOIN FETCH ar.approvers " +
            "LEFT JOIN FETCH ar.decidedBy " +
            "WHERE ar.status = :status")
    List<AbsenceRequest> findByStatus(@Param("status") AbsenceStatus status);

    @Query("SELECT DISTINCT ar FROM AbsenceRequest ar " +
            "LEFT JOIN FETCH ar.user " +
            "LEFT JOIN FETCH ar.comments c " +
            "LEFT JOIN FETCH c.createdBy " +
            "LEFT JOIN FETCH ar.approvers " +
            "LEFT JOIN FETCH ar.decidedBy " +
            "WHERE ar.status = :status " +
            "AND EXISTS (SELECT 1 FROM ar.approvers app WHERE app.id = :userId)")
    List<AbsenceRequest> findPendingByApprover(
            @Param("status") AbsenceStatus status,
            @Param("userId") Long userId);

    @Query("SELECT DISTINCT ar FROM AbsenceRequest ar " +
            "LEFT JOIN FETCH ar.user " +
            "LEFT JOIN FETCH ar.comments c " +
            "LEFT JOIN FETCH c.createdBy " +
            "LEFT JOIN FETCH ar.approvers " +
            "LEFT JOIN FETCH ar.decidedBy " +
            "WHERE ar.user.id IN (" +
            "  SELECT m.id FROM Team t JOIN t.members m WHERE t.id IN :teamIds" +
            ") AND ar.status = :status")
    List<AbsenceRequest> findByStatusAndTeams(
            @Param("status") AbsenceStatus status,
            @Param("teamIds") List<Long> teamIds);
}

package heritage.africca.accel_connect.repository;

import heritage.africca.accel_connect.entity.LeaveRequest;
import heritage.africca.accel_connect.entity.enumeration.LeaveTypeCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Integer> {

    @Query("SELECT DISTINCT lr FROM LeaveRequest lr " +
            "LEFT JOIN FETCH lr.leaves " +
            "LEFT JOIN FETCH lr.user " +
            "WHERE lr.id = :id")
    Optional<LeaveRequest> findByIdWithAssociations(@Param("id") Integer id);

    @Query("SELECT DISTINCT lr FROM LeaveRequest lr " +
            "JOIN FETCH lr.leaves l " +
            "WHERE lr.user.id = :userId AND lr.leaveType = :leaveType " +
            "AND l.date >= :fromDate AND l.status IN (heritage.africca.accel_connect.entity.enumeration.LeaveStatus.PENDING_APPROVAL, heritage.africca.accel_connect.entity.enumeration.LeaveStatus.SCHEDULED)")
    List<LeaveRequest> findPendingByUserAndLeaveType(
            @Param("userId") Long userId,
            @Param("leaveType") LeaveTypeCode leaveType,
            @Param("fromDate") LocalDate fromDate);

    @Query("SELECT DISTINCT lr FROM LeaveRequest lr " +
            "LEFT JOIN FETCH lr.leaves " +
            "WHERE lr.user.id = :userId")
    List<LeaveRequest> findByUserIdWithDetails(@Param("userId") Long userId);

    @Query("SELECT DISTINCT lr FROM LeaveRequest lr " +
            "JOIN FETCH lr.leaves l " +
            "WHERE lr.user.id = :userId " +
            "AND l.date >= :fromDate " +
            "AND l.status IN (heritage.africca.accel_connect.entity.enumeration.LeaveStatus.PENDING_APPROVAL, heritage.africca.accel_connect.entity.enumeration.LeaveStatus.SCHEDULED)")
    List<LeaveRequest> findPendingAndScheduledByUser(
            @Param("userId") Long userId,
            @Param("fromDate") LocalDate fromDate);

    @Query("SELECT DISTINCT lr FROM LeaveRequest lr " +
            "JOIN FETCH lr.leaves l " +
            "WHERE lr.user.id = :userId " +
            "AND l.date >= :fromDate " +
            "AND l.status = heritage.africca.accel_connect.entity.enumeration.LeaveStatus.PENDING_APPROVAL")
    List<LeaveRequest> findPendingByUser(
            @Param("userId") Long userId,
            @Param("fromDate") LocalDate fromDate);

    @Query("SELECT DISTINCT lr FROM LeaveRequest lr " +
            "LEFT JOIN FETCH lr.leaves " +
            "LEFT JOIN FETCH lr.user " +
            "WHERE EXISTS (" +
            "  SELECT 1 FROM Leave lv WHERE lv.leaveRequest = lr " +
            "  AND lv.status = heritage.africca.accel_connect.entity.enumeration.LeaveStatus.PENDING_APPROVAL" +
            ")")
    List<LeaveRequest> findAllWithPendingLeaves();

    @Query("SELECT DISTINCT lr FROM LeaveRequest lr " +
            "LEFT JOIN FETCH lr.leaves " +
            "LEFT JOIN FETCH lr.user " +
            "WHERE EXISTS (" +
            "  SELECT 1 FROM Leave lv WHERE lv.leaveRequest = lr " +
            "  AND lv.status = heritage.africca.accel_connect.entity.enumeration.LeaveStatus.PENDING_APPROVAL" +
            ") AND EXISTS (" +
            "  SELECT 1 FROM lr.approvers app WHERE app.id = :userId" +
            ")")
    List<LeaveRequest> findPendingLeavesByApprover(@Param("userId") Long userId);

    @Query("SELECT DISTINCT lr FROM LeaveRequest lr " +
            "LEFT JOIN FETCH lr.leaves " +
            "LEFT JOIN FETCH lr.user " +
            "WHERE EXISTS (" +
            "  SELECT 1 FROM Leave lv WHERE lv.leaveRequest = lr " +
            "  AND lv.status IN (heritage.africca.accel_connect.entity.enumeration.LeaveStatus.SCHEDULED, " +
            "                    heritage.africca.accel_connect.entity.enumeration.LeaveStatus.TAKEN)" +
            ")")
    List<LeaveRequest> findAllWithApprovedLeaves();

    @Query("SELECT DISTINCT lr FROM LeaveRequest lr " +
            "LEFT JOIN FETCH lr.leaves " +
            "LEFT JOIN FETCH lr.user " +
            "WHERE EXISTS (" +
            "  SELECT 1 FROM Leave lv WHERE lv.leaveRequest = lr " +
            "  AND lv.status IN (heritage.africca.accel_connect.entity.enumeration.LeaveStatus.SCHEDULED, " +
            "                    heritage.africca.accel_connect.entity.enumeration.LeaveStatus.TAKEN)" +
            ") AND EXISTS (" +
            "  SELECT 1 FROM lr.approvers app WHERE app.id = :userId" +
            ")")
    List<LeaveRequest> findApprovedLeavesByApprover(@Param("userId") Long userId);

    @Query("SELECT DISTINCT lr FROM LeaveRequest lr " +
            "LEFT JOIN FETCH lr.leaves " +
            "LEFT JOIN FETCH lr.user " +
            "WHERE EXISTS (" +
            "  SELECT 1 FROM Leave lv WHERE lv.leaveRequest = lr " +
            "  AND lv.status = heritage.africca.accel_connect.entity.enumeration.LeaveStatus.REJECTED" +
            ")")
    List<LeaveRequest> findAllWithRejectedLeaves();

    @Query("SELECT DISTINCT lr FROM LeaveRequest lr " +
            "LEFT JOIN FETCH lr.leaves " +
            "LEFT JOIN FETCH lr.user " +
            "WHERE EXISTS (" +
            "  SELECT 1 FROM Leave lv WHERE lv.leaveRequest = lr " +
            "  AND lv.status = heritage.africca.accel_connect.entity.enumeration.LeaveStatus.REJECTED" +
            ") AND EXISTS (" +
            "  SELECT 1 FROM lr.approvers app WHERE app.id = :userId" +
            ")")
    List<LeaveRequest> findRejectedLeavesByApprover(@Param("userId") Long userId);

    @Query("SELECT DISTINCT lr FROM LeaveRequest lr " +
            "LEFT JOIN FETCH lr.leaves " +
            "LEFT JOIN FETCH lr.user " +
            "WHERE lr.user.id IN (" +
            "  SELECT m.id FROM Team t JOIN t.members m WHERE t.id IN :teamIds" +
            ") AND EXISTS (" +
            "  SELECT 1 FROM Leave lv WHERE lv.leaveRequest = lr " +
            "  AND lv.status IN (" +
            "    heritage.africca.accel_connect.entity.enumeration.LeaveStatus.SCHEDULED," +
            "    heritage.africca.accel_connect.entity.enumeration.LeaveStatus.TAKEN)" +
            ")")
    List<LeaveRequest> findApprovedByTeams(@Param("teamIds") List<Long> teamIds);

    @Query("SELECT DISTINCT lr FROM LeaveRequest lr " +
            "LEFT JOIN FETCH lr.leaves " +
            "LEFT JOIN FETCH lr.user " +
            "WHERE lr.user.id IN (" +
            "  SELECT m.id FROM Team t JOIN t.members m WHERE t.id IN :teamIds" +
            ") AND EXISTS (" +
            "  SELECT 1 FROM Leave lv WHERE lv.leaveRequest = lr " +
            "  AND lv.status = heritage.africca.accel_connect.entity.enumeration.LeaveStatus.REJECTED" +
            ")")
    List<LeaveRequest> findRejectedByTeams(@Param("teamIds") List<Long> teamIds);
}

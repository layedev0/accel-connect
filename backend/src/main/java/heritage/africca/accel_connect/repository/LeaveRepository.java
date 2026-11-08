package heritage.africca.accel_connect.repository;

import heritage.africca.accel_connect.entity.Leave;
import heritage.africca.accel_connect.entity.enumeration.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface LeaveRepository extends JpaRepository<Leave, Integer> {

    @Query("SELECT COALESCE(SUM(l.lengthDays), 0) FROM Leave l " +
           "WHERE l.user.id = :userId " +
           "AND l.status IN :statuses " +
           "AND l.lengthDays IS NOT NULL")
    BigDecimal sumApprovedDaysByUser(
            @Param("userId") Long userId,
            @Param("statuses") List<LeaveStatus> statuses);

    @Query("SELECT COALESCE(SUM(l.lengthDays), 0) FROM Leave l " +
           "WHERE l.status IN :statuses " +
           "AND l.lengthDays IS NOT NULL")
    BigDecimal sumApprovedDaysAll(@Param("statuses") List<LeaveStatus> statuses);
}

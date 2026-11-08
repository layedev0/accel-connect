package heritage.africca.accel_connect.repository;

import heritage.africca.accel_connect.entity.User;
import heritage.africca.accel_connect.entity.WeeklyBudget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeeklyBudgetRepository extends JpaRepository<WeeklyBudget, Long> {

    Optional<WeeklyBudget> findByUserAndWeekStartDate(User user, LocalDate weekStartDate);

    List<WeeklyBudget> findByUserOrderByWeekStartDateDesc(User user);

    List<WeeklyBudget> findByWeekStartDateOrderByUserUsernameAsc(LocalDate weekStartDate);

    @Query("SELECT wb FROM WeeklyBudget wb WHERE wb.deliveryFeesCalculated = false AND wb.weekEndDate < :currentDate")
    List<WeeklyBudget> findUncalculatedBudgets(@Param("currentDate") LocalDate currentDate);

    @Query("SELECT wb FROM WeeklyBudget wb WHERE wb.depositedAmount < 0")
    List<WeeklyBudget> findAllInDebt();

    @Query("SELECT COALESCE(SUM(wb.depositedAmount), 0) FROM WeeklyBudget wb WHERE wb.weekStartDate = :weekStart")
    Double getTotalDepositedForWeek(@Param("weekStart") LocalDate weekStart);

    @Query("SELECT COALESCE(SUM(wb.spentAmount), 0) FROM WeeklyBudget wb WHERE wb.weekStartDate = :weekStart")
    Double getTotalSpentForWeek(@Param("weekStart") LocalDate weekStart);
}

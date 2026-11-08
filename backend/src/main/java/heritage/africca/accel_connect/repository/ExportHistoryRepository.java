package heritage.africca.accel_connect.repository;

import heritage.africca.accel_connect.entity.ExportHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExportHistoryRepository extends JpaRepository<ExportHistory, Long> {

    List<ExportHistory> findAllByOrderByExportDateDesc();

    List<ExportHistory> findByWeekStartDateAndWeekEndDate(LocalDate startDate, LocalDate endDate);

    boolean existsByWeekStartDateAndWeekEndDate(LocalDate startDate, LocalDate endDate);

    Optional<ExportHistory> findTopByOrderByExportDateDesc();

    List<ExportHistory> findByExportDateBefore(LocalDateTime cutoffDate);

    List<ExportHistory> findByExportedBy(String exportedBy);

    List<ExportHistory> findByWeekStartDateGreaterThanEqual(LocalDate startDate);
}
package heritage.africca.accel_connect.service.Impl;

import heritage.africca.accel_connect.entity.ExportHistory;
import heritage.africca.accel_connect.entity.enumeration.OrderStatus;
import heritage.africca.accel_connect.repository.ExportHistoryRepository;
import heritage.africca.accel_connect.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 📜 SERVICE DE GESTION DE L'HISTORIQUE DES EXPORTS
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ExportHistoryService {

    private final ExportHistoryRepository exportHistoryRepository;
    private final OrderRepository orderRepository;

    /**
     * 💾 Enregistre un export dans l'historique
     */
    @Transactional
    public ExportHistory recordExport(
            LocalDate weekStartDate,
            LocalDate weekEndDate,
            String fileName,
            long fileSize,
            String exportedBy
    ) {
        ExportHistory history = new ExportHistory();
        history.setWeekStartDate(weekStartDate);
        history.setWeekEndDate(weekEndDate);
        history.setFileName(fileName);
        history.setFileSize(fileSize);
        history.setExportedBy(exportedBy);

        ExportHistory saved = exportHistoryRepository.save(history);
        log.info("💾 Export enregistré dans l'historique: {} (ID: {})", fileName, saved.getId());

        return saved;
    }

    /**
     * 📋 Récupère tous les exports
     */
    @Transactional(readOnly = true)
    public List<ExportHistory> getAllExports() {
        return exportHistoryRepository.findAllByOrderByExportDateDesc();
    }

    /**
     * 🔍 Récupère un export par son ID
     */
    @Transactional(readOnly = true)
    public Optional<ExportHistory> getExportById(Long id) {
        return exportHistoryRepository.findById(id);
    }

    /**
     * 📋 Récupère les exports pour une période spécifique
     */
    @Transactional(readOnly = true)
    public List<ExportHistory> getExportsByPeriod(LocalDate startDate, LocalDate endDate) {
        return exportHistoryRepository.findByWeekStartDateAndWeekEndDate(startDate, endDate);
    }

    /**
     * ✅ Vérifie si une semaine a déjà été exportée
     */
    @Transactional(readOnly = true)
    public boolean hasBeenExported(LocalDate weekStartDate, LocalDate weekEndDate) {
        return exportHistoryRepository.existsByWeekStartDateAndWeekEndDate(weekStartDate, weekEndDate);
    }

    /**
     * 📅 Récupère la dernière date d'export
     */
    @Transactional(readOnly = true)
    public Optional<LocalDate> getLastExportDate() {
        return exportHistoryRepository.findTopByOrderByExportDateDesc()
                .map(history -> history.getExportDate().toLocalDate());
    }

    /**
     * 🗑️ Supprime les anciens exports (plus de 6 mois)
     */
    @Transactional
    public int cleanupOldExports() {
        LocalDate cutoffDate = LocalDate.now().minusMonths(6);
        List<ExportHistory> oldExports = exportHistoryRepository
                .findByExportDateBefore(cutoffDate.atStartOfDay());

        int count = oldExports.size();
        exportHistoryRepository.deleteAll(oldExports);

        log.info("🗑️ {} ancien(s) export(s) supprimé(s)", count);
        return count;
    }

    /**
     * 📊 Récupère les statistiques d'export
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getExportStatistics() {
        List<ExportHistory> allExports = exportHistoryRepository.findAll();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalExports", allExports.size());
        stats.put("totalSize", allExports.stream().mapToLong(ExportHistory::getFileSize).sum());
        stats.put("lastExport", allExports.stream()
                .max(Comparator.comparing(ExportHistory::getExportDate))
                .map(h -> h.getExportDate().toString())
                .orElse(null));

        // Grouper par utilisateur
        Map<String, Long> byUser = allExports.stream()
                .collect(Collectors.groupingBy(
                        ExportHistory::getExportedBy,
                        Collectors.counting()
                ));
        stats.put("exportsByUser", byUser);

        return stats;
    }
}
package heritage.africca.accel_connect.controller;

import heritage.africca.accel_connect.dto.WeeklyExportDTO;
import heritage.africca.accel_connect.entity.ExportHistory;
import heritage.africca.accel_connect.service.Impl.ExcelExportService;
import heritage.africca.accel_connect.service.Impl.WeeklyExportService;
import heritage.africca.accel_connect.service.Impl.ExportHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 📥 CONTRÔLEUR D'EXPORT EXCEL AMÉLIORÉ
 */
@RestController
@RequestMapping("/api/v1/admin/export")
@CrossOrigin("*")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')")
@Slf4j
public class EnhancedExcelExportController {

    private final WeeklyExportService weeklyExportService;
    private final ExcelExportService excelExportService;
    private final ExportHistoryService exportHistoryService;

    /**
     * 📋 RÉCUPÈRE LA LISTE DES SEMAINES DISPONIBLES POUR EXPORT
     */
    @GetMapping("/available-weeks")
    public ResponseEntity<List<Map<String, Object>>> getAvailableWeeks() {
        try {
            log.info("📋 Récupération des semaines disponibles");

            List<Map<String, Object>> weeks = weeklyExportService.getAvailableWeeks();

            log.info("✅ {} semaine(s) disponible(s) trouvée(s)", weeks.size());
            return ResponseEntity.ok(weeks);

        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des semaines", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }

    /**
     * 📥 TÉLÉCHARGE LE RAPPORT EXCEL POUR UNE SEMAINE SPÉCIFIQUE
     * Retourne le fichier en Base64 dans un JSON
     */
    @GetMapping("/download-week")
    public ResponseEntity<?> downloadWeekReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication
    ) {
        try {
            log.info("📥 Téléchargement rapport du {} au {}", startDate, endDate);

            // 1️⃣ Préparer les données
            WeeklyExportDTO exportData = weeklyExportService.prepareWeeklyExportData(startDate, endDate);

            // 2️⃣ Générer le fichier Excel
            byte[] excelBytes = excelExportService.generateWeeklyReport(exportData);

            // 3️⃣ Créer le nom du fichier
            String fileName = String.format("Commandes_Semaine_%s_au_%s.xlsx",
                    startDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")),
                    endDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
            );

            // 4️⃣ Encoder en Base64
            String base64Excel = Base64.getEncoder().encodeToString(excelBytes);

            // 5️⃣ Enregistrer dans l'historique
            String username = authentication.getName();
            exportHistoryService.recordExport(startDate, endDate, fileName, excelBytes.length, username);

            // 6️⃣ Créer la réponse
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("excel", Map.of(
                    "fileName", fileName,
                    "data", base64Excel,
                    "size", excelBytes.length
            ));
            response.put("weekInfo", Map.of(
                    "startDate", startDate.toString(),
                    "endDate", endDate.toString(),
                    "totalOrders", exportData.getGlobalStats().getTotalOrders(),
                    "totalAmount", exportData.getGlobalStats().getTotalAmountWithSubsidy()
            ));

            log.info("✅ Export réussi - Fichier: {} ({} octets)", fileName, excelBytes.length);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Erreur lors du téléchargement", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Erreur lors de la génération du rapport: " + e.getMessage()
                    ));
        }
    }

    /**
     * 📜 RÉCUPÈRE L'HISTORIQUE DES EXPORTS
     */
    @GetMapping("/history")
    public ResponseEntity<List<Map<String, Object>>> getExportHistory() {
        try {
            log.info("📜 Récupération de l'historique des exports");

            List<ExportHistory> history = exportHistoryService.getAllExports();

            List<Map<String, Object>> response = history.stream()
                    .map(h -> Map.of(
                            "id", (Object) h.getId(),
                            "weekStartDate", h.getWeekStartDate().toString(),
                            "weekEndDate", h.getWeekEndDate().toString(),
                            "exportDate", h.getExportDate().toString(),
                            "fileName", h.getFileName(),
                            "fileSize", h.getFileSize(),
                            "exportedBy", h.getExportedBy()
                    ))
                    .toList();

            log.info("✅ {} export(s) trouvé(s) dans l'historique", response.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération de l'historique", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }

    /**
     * 📥 RE-TÉLÉCHARGE UN EXPORT DEPUIS L'HISTORIQUE
     */
    @GetMapping("/redownload/{historyId}")
    public ResponseEntity<?> redownloadFromHistory(@PathVariable Long historyId) {
        try {
            log.info("📥 Re-téléchargement depuis l'historique #{}", historyId);

            ExportHistory history = exportHistoryService.getExportById(historyId)
                    .orElseThrow(() -> new RuntimeException("Export introuvable"));

            // Régénérer le fichier
            WeeklyExportDTO exportData = weeklyExportService.prepareWeeklyExportData(
                    history.getWeekStartDate(),
                    history.getWeekEndDate()
            );

            byte[] excelBytes = excelExportService.generateWeeklyReport(exportData);
            String base64Excel = Base64.getEncoder().encodeToString(excelBytes);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("excel", Map.of(
                    "fileName", history.getFileName(),
                    "data", base64Excel,
                    "size", excelBytes.length
            ));
            response.put("weekInfo", Map.of(
                    "startDate", history.getWeekStartDate().toString(),
                    "endDate", history.getWeekEndDate().toString(),
                    "totalOrders", exportData.getGlobalStats().getTotalOrders(),
                    "totalAmount", exportData.getGlobalStats().getTotalAmountWithSubsidy()
            ));

            log.info("✅ Re-téléchargement réussi - {}", history.getFileName());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Erreur lors du re-téléchargement", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Erreur lors du re-téléchargement: " + e.getMessage()
                    ));
        }
    }

//    /**
//     * 📊 PRÉVISUALISE LES DONNÉES QUI SERONT EXPORTÉES
//     */
//    @GetMapping("/weekly-preview")
//    public ResponseEntity<WeeklyExportDTO> previewWeeklyReport(
//            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
//            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
//    ) {
//        try {
//            if (startDate == null || endDate == null) {
//                LocalDate[] currentWeek = weeklyExportService.getCurrentWeek();
//                startDate = currentWeek[0];
//                endDate = currentWeek[1];
//            }
//
//            WeeklyExportDTO exportData = weeklyExportService.prepareWeeklyExportData(startDate, endDate);
//            return ResponseEntity.ok(exportData);
//
//        } catch (Exception e) {
//            log.error("❌ Erreur lors de la prévisualisation", e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
//        }
//    }
}
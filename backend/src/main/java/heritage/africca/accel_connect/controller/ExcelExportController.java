package heritage.africca.accel_connect.controller;

import heritage.africca.accel_connect.dto.WeeklyExportDTO;
import heritage.africca.accel_connect.service.Impl.ExcelExportService;
import heritage.africca.accel_connect.service.Impl.WeeklyExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 📥 CONTRÔLEUR D'EXPORT EXCEL
 */
@RestController
@RequestMapping("/api/v1/admin/export")
@CrossOrigin("*")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')")
@Slf4j
public class ExcelExportController {

    private final WeeklyExportService weeklyExportService;
    private final ExcelExportService excelExportService;

    /**
     * 📥 TÉLÉCHARGE LE RAPPORT HEBDOMADAIRE EN EXCEL
     * ⚠️ Si aucune date fournie, utilise la semaine en cours
     */
    @GetMapping("/weekly-report")
    public ResponseEntity<byte[]> downloadWeeklyReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        try {
            // Si aucune date fournie, utiliser la semaine en cours
            if (startDate == null || endDate == null) {
                LocalDate[] currentWeek = weeklyExportService.getCurrentWeek();
                startDate = currentWeek[0];
                endDate = currentWeek[1];
                log.info("📅 Utilisation de la semaine en cours: {} au {}", startDate, endDate);
            }

            // Préparer les données
            WeeklyExportDTO exportData = weeklyExportService.prepareWeeklyExportData(startDate, endDate);

            // Générer le fichier Excel
            byte[] excelBytes = excelExportService.generateWeeklyReport(exportData);

            // Créer le nom de fichier
            String fileName = String.format("Commandes_Semaine_%s_au_%s.xlsx",
                    startDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")),
                    endDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
            );

            // Préparer la réponse HTTP
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", fileName);
            headers.setContentLength(excelBytes.length);

            log.info("✅ Export Excel généré: {} ({} octets)", fileName, excelBytes.length);

            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);

        } catch (IOException e) {
            log.error("❌ Erreur lors de la génération du fichier Excel", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        } catch (Exception e) {
            log.error("❌ Erreur inattendue lors de l'export", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    /**
     * 📊 PRÉVISUALISE LES DONNÉES QUI SERONT EXPORTÉES
     * Utile pour vérifier avant de télécharger
     */
    @GetMapping("/weekly-preview")
    public ResponseEntity<WeeklyExportDTO> previewWeeklyReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        try {
            // Si aucune date fournie, utiliser la semaine en cours
            if (startDate == null || endDate == null) {
                LocalDate[] currentWeek = weeklyExportService.getCurrentWeek();
                startDate = currentWeek[0];
                endDate = currentWeek[1];
            }

            WeeklyExportDTO exportData = weeklyExportService.prepareWeeklyExportData(startDate, endDate);
            return ResponseEntity.ok(exportData);

        } catch (Exception e) {
            log.error("❌ Erreur lors de la prévisualisation", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }
}
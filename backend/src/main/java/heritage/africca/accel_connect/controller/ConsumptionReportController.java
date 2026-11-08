package heritage.africca.accel_connect.controller;

import heritage.africca.accel_connect.service.Impl.ConsumptionReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Reports", description = "Consumption reports endpoints")
public class ConsumptionReportController {

    private final ConsumptionReportService consumptionReportService;

    @GetMapping("/consumption/excel")
    @PreAuthorize("hasRole('RESTAURANT_MANAGER') or hasRole('ADMIN')")
    @Operation(summary = "Download consumption report as Excel",
              description = "Generate and download a formatted Excel report with consumption data. " +
                           "Period type: 'all' for all data, 'week' for specific week, 'day' for specific day")
    public ResponseEntity<byte[]> downloadConsumptionReport(
            @RequestParam(defaultValue = "all") String periodType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) throws IOException {

        log.info("Consumption report requested - type: {}, start: {}, end: {}", periodType, startDate, endDate);

        // Déterminer les dates selon le type de période
        LocalDate finalStartDate;
        LocalDate finalEndDate;

        switch (periodType.toLowerCase()) {
            case "day":
                if (startDate == null) {
                    startDate = LocalDate.now();
                }
                finalStartDate = startDate;
                finalEndDate = startDate;
                break;

            case "week":
                if (startDate == null) {
                    // Semaine courante (lundi à vendredi)
                    LocalDate today = LocalDate.now();
                    int dayOfWeek = today.getDayOfWeek().getValue(); // 1=Lundi, 7=Dimanche
                    finalStartDate = today.minusDays(dayOfWeek - 1); // Lundi
                    finalEndDate = finalStartDate.plusDays(4); // Vendredi
                } else {
                    // Semaine spécifique à partir de startDate
                    finalStartDate = startDate;
                    finalEndDate = endDate != null ? endDate : startDate.plusDays(4);
                }
                break;

            case "all":
            default:
                // Toutes les données depuis le début
                finalStartDate = LocalDate.of(2020, 1, 1);
                finalEndDate = LocalDate.now();
                break;
        }

        // Générer le rapport Excel
        byte[] excelData = consumptionReportService.generateConsumptionReport(
                periodType, finalStartDate, finalEndDate);

        // Créer le nom du fichier
        String filename = generateFilename(periodType, finalStartDate, finalEndDate);

        // Préparer la réponse
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(excelData.length);

        log.info("Consumption report generated successfully: {}", filename);

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
    }

    private String generateFilename(String periodType, LocalDate startDate, LocalDate endDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String dateStr;

        switch (periodType.toLowerCase()) {
            case "day":
                dateStr = startDate.format(formatter);
                return String.format("rapport_consommation_%s.xlsx", dateStr);

            case "week":
                String start = startDate.format(formatter);
                String end = endDate.format(formatter);
                return String.format("rapport_consommation_%s_au_%s.xlsx", start, end);

            case "all":
            default:
                return String.format("rapport_complet_consommation_%s.xlsx",
                        LocalDate.now().format(formatter));
        }
    }
}

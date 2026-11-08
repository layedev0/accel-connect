package heritage.africca.accel_connect.controller;

import heritage.africca.accel_connect.dto.PaymentSummary;
import heritage.africca.accel_connect.dto.PaymentValidationRequest;
import heritage.africca.accel_connect.dto.PaymentValidationResponse;
import heritage.africca.accel_connect.service.PaymentService;
import heritage.africca.accel_connect.service.Impl.ExcelExportService;
import heritage.africca.accel_connect.service.Impl.WeeklyExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.Map;

/**
 * 💰 CONTRÔLEUR DE PAIEMENT ET VALIDATION HEBDOMADAIRE
 */
@RestController
@RequestMapping("/api/v1/admin/payments")
@CrossOrigin("*")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')")
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final WeeklyExportService weeklyExportService;
    private final ExcelExportService excelExportService;

    /**
     * ✅ VÉRIFIE SI ON PEUT VALIDER LES PAIEMENTS (VENDREDI UNIQUEMENT)
     */
    @GetMapping("/can-validate")
    public ResponseEntity<Map<String, Object>> canValidatePayments() {
        Map<String, Object> response = new HashMap<>();

        LocalDate today = LocalDate.now();
        boolean isFriday = today.getDayOfWeek() == DayOfWeek.FRIDAY;

        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate friday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));

        response.put("canValidate", isFriday);
        response.put("isFriday", isFriday);
        response.put("currentDay", today.getDayOfWeek().name());
        response.put("weekStartDate", monday.toString());
        response.put("weekEndDate", friday.toString());

        if (!isFriday) {
            response.put("message", "La validation des paiements ne peut être effectuée que le VENDREDI");
            response.put("nextFriday", friday.toString());
        } else {
            response.put("message", "Vous pouvez valider les paiements de la semaine");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 📊 GÉNÈRE LE RÉCAPITULATIF DE PAIEMENT DE LA SEMAINE EN COURS
     * ⚠️ NE FONCTIONNE QUE LE VENDREDI
     */
    @GetMapping("/summary")
    public ResponseEntity<PaymentSummary> getPaymentSummary() {
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate friday = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));

        PaymentSummary summary = paymentService.generatePaymentSummary(monday, friday);
        return ResponseEntity.ok(summary);
    }

    /**
     * ✅ VALIDE ET ENREGISTRE LE PAIEMENT + TÉLÉCHARGE AUTOMATIQUEMENT LE RAPPORT EXCEL
     * 🎯 NOUVELLE FONCTIONNALITÉ: Retourne un JSON avec le fichier Excel en base64
     */
    @PostMapping("/validate-and-download")
    public ResponseEntity<?> validatePaymentAndDownloadReport(
            @RequestBody PaymentValidationRequest request
    ) {
        try {
            log.info("🚀 Début validation paiement + génération Excel");

            // 1️⃣ Valider le paiement
            PaymentValidationResponse validationResponse = paymentService.validatePayment(request);

            // 2️⃣ Générer le rapport Excel
            LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate friday = LocalDate.now();

            var exportData = weeklyExportService.prepareWeeklyExportData(monday, friday);
            byte[] excelBytes = excelExportService.generateWeeklyReport(exportData);

            // 3️⃣ Créer le nom du fichier
            String fileName = String.format("Cloture_Semaine_%s_au_%s.xlsx",
                    monday.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")),
                    friday.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
            );

            // 4️⃣ Encoder en Base64 pour envoi JSON
            String base64Excel = java.util.Base64.getEncoder().encodeToString(excelBytes);

            // 5️⃣ Créer la réponse combinée
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("validation", validationResponse);
            response.put("excel", Map.of(
                    "fileName", fileName,
                    "data", base64Excel,
                    "size", excelBytes.length
            ));

            log.info("✅ Validation + Export réussi - Fichier: {} ({} octets)", fileName, excelBytes.length);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("❌ Erreur génération Excel", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Erreur lors de la génération du rapport Excel: " + e.getMessage()
                    ));
        } catch (Exception e) {
            log.error("❌ Erreur validation paiement", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Erreur lors de la validation: " + e.getMessage()
                    ));
        }
    }

    /**
     * ✅ ANCIENNE MÉTHODE DE VALIDATION (conservée pour compatibilité)
     */
    @PostMapping("/validate")
    public ResponseEntity<PaymentValidationResponse> validatePayment(
            @RequestBody PaymentValidationRequest request
    ) {
        PaymentValidationResponse response = paymentService.validatePayment(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 📅 RÉCUPÈRE LA DATE DU DERNIER PAIEMENT
     */
    @GetMapping("/last-payment-date")
    public ResponseEntity<Map<String, String>> getLastPaymentDate() {
        Map<String, String> response = new HashMap<>();
        paymentService.getLastPaymentDate()
                .ifPresentOrElse(
                        date -> response.put("lastPaymentDate", date.toString()),
                        () -> response.put("lastPaymentDate", null)
                );
        return ResponseEntity.ok(response);
    }
}
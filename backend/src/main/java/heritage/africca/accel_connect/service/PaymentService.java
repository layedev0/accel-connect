package heritage.africca.accel_connect.service;

import heritage.africca.accel_connect.dto.PaymentSummary;
import heritage.africca.accel_connect.dto.PaymentValidationRequest;
import heritage.africca.accel_connect.dto.PaymentValidationResponse;

import java.time.LocalDate;
import java.util.Optional;

public interface PaymentService {

    /**
     * 📊 Génère le récapitulatif de paiement pour une période
     * ⚠️ Ne peut être appelé que le VENDREDI
     */
    PaymentSummary generatePaymentSummary(LocalDate startDate, LocalDate endDate);

    /**
     * ✅ Valide et enregistre le paiement
     * 🚚 Calcule automatiquement les frais de livraison
     * ⚠️ Ne peut être appelé que le VENDREDI
     */
    PaymentValidationResponse validatePayment(PaymentValidationRequest request);

    /**
     * 📅 Récupère la date du dernier paiement
     */
    Optional<LocalDate> getLastPaymentDate();

    /**
     * ✅ Vérifie si on peut valider les paiements (vendredi uniquement)
     */
    boolean canValidatePayments();
}
package heritage.africca.accel_connect.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 💸 DTO POUR AJOUTER UNE DETTE À UN UTILISATEUR
 */
@Data
public class AddDebtRequest {

    @NotNull(message = "L'ID de l'utilisateur est requis")
    private Long userId;

    @NotNull(message = "Le montant de la dette est requis")
    @Positive(message = "Le montant de la dette doit être positif")
    private Double debtAmount;

    @NotNull(message = "La raison de la dette est requise")
    private String reason;
}
package heritage.africca.accel_connect.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 📝 DTO pour la mise à jour de la quantité d'un plat
 */
@Data
public class OrderItemUpdateRequest {

    @NotNull(message = "La nouvelle quantité est requise")
    @Min(value = 1, message = "La quantité doit être au moins 1")
    private Integer newQuantity;

    private String reason; // Raison de la modification (optionnel)
}
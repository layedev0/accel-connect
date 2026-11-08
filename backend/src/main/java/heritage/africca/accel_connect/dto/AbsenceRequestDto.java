package heritage.africca.accel_connect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AbsenceRequestDto {

    @NotBlank(message = "Le type d'absence est obligatoire")
    private String absenceType;

    @NotNull(message = "La date est obligatoire")
    private LocalDate date;

    @NotNull(message = "L'heure de début est obligatoire")
    private LocalTime startTime;

    @NotNull(message = "L'heure de fin est obligatoire")
    private LocalTime endTime;

    @NotBlank(message = "Le motif est obligatoire")
    private String reason;

    @NotEmpty(message = "Au moins un approbateur est requis")
    private List<Long> approverUserIds;
}

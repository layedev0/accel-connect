package heritage.africca.accel_connect.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {

    private Instant timestamp;
    private int status;
    private String error;
    private String message;

    public static ErrorResponse of(int status, String error, String message) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status)
                .error(error)
                .message(message)
                .build();
    }
}

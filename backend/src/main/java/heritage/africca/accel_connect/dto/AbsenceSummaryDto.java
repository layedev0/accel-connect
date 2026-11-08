package heritage.africca.accel_connect.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AbsenceSummaryDto {

    private Integer id;
    private String referenceCode;
    private String employeeName;
    private String employeeEmail;
    private Long employeeUserId;
    private String absenceTypeName;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal durationHours;
    private String reason;
    private String status;
    private LocalDate dateApplied;
    private List<CommentDto> comments;
    private List<ApproverDto> approvers;
    private String decidedByName;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommentDto {
        private String text;
        private String author;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApproverDto {
        private Long id;
        private String displayName;
    }
}

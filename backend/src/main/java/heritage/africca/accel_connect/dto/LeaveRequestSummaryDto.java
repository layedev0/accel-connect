package heritage.africca.accel_connect.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRequestSummaryDto {

    private Integer id;
    private String employeeName;
    private String employeeEmail;
    private String leaveTypeCode;
    private String leaveTypeName;
    private LocalDate dateApplied;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalDays;
    private String overallStatus;
    private List<LeaveDayDetailDto> days;
    private List<CommentDto> comments;
  
    private List<ApproverDto> approvers;
    private String decidedByName;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApproverDto {
        private Long id;
        private String displayName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommentDto {
        private String text;
        private String author;
        private LocalDateTime createdAt;
    }
}

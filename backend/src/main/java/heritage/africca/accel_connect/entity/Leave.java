package heritage.africca.accel_connect.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import heritage.africca.accel_connect.converter.LeaveStatusConverter;
import heritage.africca.accel_connect.converter.LeaveTypeConverter;
import heritage.africca.accel_connect.entity.enumeration.LeaveStatus;
import heritage.africca.accel_connect.entity.enumeration.LeaveTypeCode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "leaves")
@Data
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@NoArgsConstructor
@AllArgsConstructor
public class Leave {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "length_hours", precision = 6, scale = 2)
    private BigDecimal lengthHours;

    @Column(name = "length_days", precision = 6, scale = 4)
    private BigDecimal lengthDays;

    @Convert(converter = LeaveStatusConverter.class)
    @Column(name = "status")
    private LeaveStatus status;

    @Convert(converter = LeaveTypeConverter.class)
    @Column(name = "leave_type_code", nullable = false)
    private LeaveTypeCode leaveType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_request_id", referencedColumnName = "id")
    @JsonIgnore
    private LeaveRequest leaveRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "duration_type")
    private Integer durationType;

    // Duration types
    public static final int DURATION_FULL_DAY = 0;
    public static final int DURATION_HALF_DAY = 1;
}

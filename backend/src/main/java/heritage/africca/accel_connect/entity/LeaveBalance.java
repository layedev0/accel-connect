package heritage.africca.accel_connect.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;


@Entity
@Table(name = "leave_balances")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "balance_days", nullable = false)
    private BigDecimal balanceDays = BigDecimal.ZERO;

    @Column(name = "leave_due", nullable = false)
    private BigDecimal leaveDue = BigDecimal.ZERO;

    @Column(name = "leave_taken", nullable = false)
    private BigDecimal leaveTaken = BigDecimal.ZERO;

    @Column(name = "last_updated", nullable = false)
    private LocalDate lastUpdated;


}

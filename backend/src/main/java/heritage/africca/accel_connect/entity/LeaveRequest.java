package heritage.africca.accel_connect.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import heritage.africca.accel_connect.converter.LeaveTypeConverter;
import heritage.africca.accel_connect.entity.enumeration.LeaveTypeCode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "leave_request")
@Data
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Convert(converter = LeaveTypeConverter.class)
    @Column(name = "leave_type_code", nullable = false)
    private LeaveTypeCode leaveType;

    @Column(name = "date_applied", nullable = false)
    private LocalDate dateApplied;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "leaveRequest", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Leave> leaves;

    @OneToMany(mappedBy = "leaveRequest", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LeaveRequestComment> comments;

   
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "leave_request_approvers",
            joinColumns = @JoinColumn(name = "leave_request_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> approvers;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by_user_id")
    private User decidedBy;
}

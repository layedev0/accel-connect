package heritage.africca.accel_connect.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import heritage.africca.accel_connect.converter.AbsenceStatusConverter;
import heritage.africca.accel_connect.entity.enumeration.AbsenceStatus;
import heritage.africca.accel_connect.entity.enumeration.AbsenceTypeCode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

@Entity
@Table(name = "absence_request")
@Setter
@Getter
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@NoArgsConstructor
@AllArgsConstructor
public class AbsenceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    /** Computed from endTime - startTime, max 8.00 */
    @Column(name = "duration_hours", precision = 4, scale = 2, nullable = false)
    private BigDecimal durationHours;

    @Enumerated(EnumType.STRING)
    @Column(name = "absence_type", length = 50)
    private AbsenceTypeCode absenceType;

    @Column(name = "reason",nullable = false, length = 500)
    private String reason;

    @Convert(converter = AbsenceStatusConverter.class)
    @Column(name = "status", nullable = false)
    private AbsenceStatus status;

    @Column(name = "reference_code", length = 30, unique = true)
    private String referenceCode;

    @Column(name = "date_applied", nullable = false)
    private LocalDate dateApplied;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "absenceRequest", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<AbsenceComment> comments;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "absence_request_approvers",
            joinColumns = @JoinColumn(name = "absence_request_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> approvers;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by_user_id")
    private User decidedBy;
}

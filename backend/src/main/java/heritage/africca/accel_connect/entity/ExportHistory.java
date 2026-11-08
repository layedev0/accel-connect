package heritage.africca.accel_connect.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 📜 HISTORIQUE DES EXPORTS EXCEL
 */
@Entity
@Table(name = "export_history")
@Data
public class ExportHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate weekStartDate;

    @Column(nullable = false)
    private LocalDate weekEndDate;

    /**
     * ⚠️ CORRECTION: Retirer @CreatedDate et utiliser @PrePersist
     * pour gérer manuellement la date d'export
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime exportDate;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false, length = 100)
    private String exportedBy;

    @Column(length = 500)
    private String notes;

    /**
     * 🔧 Initialise automatiquement exportDate lors de la création
     */
    @PrePersist
    protected void onCreate() {
        if (this.exportDate == null) {
            this.exportDate = LocalDateTime.now();
        }
    }
}
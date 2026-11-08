package heritage.africca.accel_connect.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.List;

@Entity
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "montant")
    private double montant;

    @OneToOne
    @JoinColumn(name = "id_facture")
    private Facture facture;
}

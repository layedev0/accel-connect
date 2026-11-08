package heritage.africca.accel_connect.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Compte  {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "solde")
    private Double solde;

    @Column(name = "transact_type", nullable = false)
    private String transactType;

    @OneToOne
    @JoinColumn(name = "id_customer")
    private Customer customer;

    @JoinColumn(name = "operation_date", nullable = false)
    private LocalDateTime operationDate;

}

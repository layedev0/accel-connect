package heritage.africca.accel_connect.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "facture")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Facture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date")
    private LocalDateTime date;

    @OneToOne
    @JoinColumn(name = "id_order")
    private Order order;

    private double totalAmount;

}


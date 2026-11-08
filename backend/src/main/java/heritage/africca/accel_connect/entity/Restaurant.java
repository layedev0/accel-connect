package heritage.africca.accel_connect.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Restaurant implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurant_name")
    private String restaurantName;

    private String address;
    private String contact;

    @Column(name = "delivery_fee", nullable = false)
    private Double deliveryFee;

    @Column(nullable = false)
    private Boolean ordersBlocked = false;

    @Column(length = 500)
    private String blockMessage;

    @ManyToMany
    @JoinTable(
            name = "provisionner",
            joinColumns = @JoinColumn(name = "restaurant_id"),
            inverseJoinColumns = @JoinColumn(name = "meal_id")
    )
    @JsonIgnoreProperties("restaurants")
    private List<Meal> meals = new ArrayList<>();

    public boolean isOrdersAllowed() {
        return !ordersBlocked;
    }

    public String getBlockMessageOrDefault() {
        if (blockMessage != null && !blockMessage.trim().isEmpty()) {
            return blockMessage;
        }
        return "Restaurant Fermé pour Aujourd'hui!";
    }
}
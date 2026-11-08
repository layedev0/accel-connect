package heritage.africca.accel_connect.repository;

import heritage.africca.accel_connect.entity.Menu;
import heritage.africca.accel_connect.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MenuRepository extends JpaRepository<Menu, Long> {

    Optional<Menu> findByRestaurantAndMenuDate(Restaurant restaurant, LocalDate menuDate);

    List<Menu> findByRestaurantId(Long restaurantId);

    List<Menu> findByMenuDateAndIsActiveTrue(LocalDate menuDate);

    List<Menu> findByRestaurantIdAndIsActiveTrue(Long restaurantId);

    boolean existsByRestaurantIdAndMenuDate(Long restaurantId, LocalDate menuDate);
}
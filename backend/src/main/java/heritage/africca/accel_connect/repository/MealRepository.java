package heritage.africca.accel_connect.repository;

import heritage.africca.accel_connect.dto.MealDTO;
import heritage.africca.accel_connect.dto.RestaurantDTO;
import heritage.africca.accel_connect.entity.Meal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MealRepository extends JpaRepository<Meal, Long> {
}

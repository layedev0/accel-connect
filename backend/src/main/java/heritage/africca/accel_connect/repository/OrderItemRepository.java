package heritage.africca.accel_connect.repository;

import heritage.africca.accel_connect.entity.OrderItem;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends CrudRepository<OrderItem, Long> {
}
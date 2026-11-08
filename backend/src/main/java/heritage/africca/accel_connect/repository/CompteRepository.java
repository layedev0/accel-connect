package heritage.africca.accel_connect.repository;

import heritage.africca.accel_connect.entity.Compte;
import heritage.africca.accel_connect.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompteRepository extends JpaRepository<Compte, Long> {
    Customer findCustomerById(Long customerId);

}

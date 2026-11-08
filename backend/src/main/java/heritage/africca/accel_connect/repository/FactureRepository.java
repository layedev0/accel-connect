package heritage.africca.accel_connect.repository;


import heritage.africca.accel_connect.entity.Facture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FactureRepository extends JpaRepository<Facture, Long> {
}

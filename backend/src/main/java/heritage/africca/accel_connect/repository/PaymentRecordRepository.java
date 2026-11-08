package heritage.africca.accel_connect.repository;

import heritage.africca.accel_connect.entity.PaymentRecord;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRecordRepository extends CrudRepository<PaymentRecord, Long> {

    /**
     * 📅 Récupère le dernier paiement effectué
     */
    @Query("SELECT p FROM PaymentRecord p ORDER BY p.endDate DESC LIMIT 1")
    Optional<PaymentRecord> findLastPayment();

    /**
     * 📅 Récupère tous les paiements dans une période
     */
    @Query("SELECT p FROM PaymentRecord p WHERE p.startDate >= :startDate AND p.endDate <= :endDate ORDER BY p.endDate DESC")
    List<PaymentRecord> findByPeriod(LocalDate startDate, LocalDate endDate);

    /**
     * 📊 Récupère tous les paiements
     */
    @Query("SELECT p FROM PaymentRecord p ORDER BY p.endDate DESC")
    List<PaymentRecord> findAllOrderByEndDateDesc();
}
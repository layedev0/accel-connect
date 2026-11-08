package heritage.africca.accel_connect.repository;

import heritage.africca.accel_connect.entity.DepositHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepositHistoryRepository extends JpaRepository<DepositHistory, Long> {

    List<DepositHistory> findTop4ByUserIdOrderByCreatedAtDesc(Long userId);
}

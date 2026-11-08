package heritage.africca.accel_connect.repository;

import heritage.africca.accel_connect.entity.UserBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserBalanceRepository extends JpaRepository<UserBalance, Long> {

    /**
     * Récupère le solde d'un utilisateur
     */
    Optional<UserBalance> findByUserId(Long userId);

    /**
     * Récupère tous les soldes avec les utilisateurs
     */
    @Query("SELECT ub FROM UserBalance ub JOIN FETCH ub.user")
    List<UserBalance> findAll();
}
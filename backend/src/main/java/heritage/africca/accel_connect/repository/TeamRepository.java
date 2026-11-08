package heritage.africca.accel_connect.repository;

import heritage.africca.accel_connect.entity.Team;
import heritage.africca.accel_connect.entity.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {

    @Query("SELECT t FROM Team t JOIN t.members m WHERE m = :user")
    Optional<Team> findByMember(@Param("user") User user);

    @Query("SELECT t FROM Team t JOIN t.supervisors s WHERE s.id = :userId")
    List<Team> findSupervisedBy(@Param("userId") Long userId);

    @Query("SELECT COUNT(t) > 0 FROM Team t JOIN t.members m WHERE t.id IN :teamIds AND m.id = :userId")
    boolean isMemberOfAnyTeam(@Param("userId") Long userId, @Param("teamIds") List<Long> teamIds);
}

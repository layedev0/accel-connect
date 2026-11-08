package heritage.africca.accel_connect.repository;

import heritage.africca.accel_connect.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByKeycloakId(String keycloakId);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findFirstByEmail(String email);

    boolean existsByKeycloakId(String keycloakId);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<User> findByInternTrue();

    Optional<User> findByFirstNameIgnoreCaseAndLastNameIgnoreCase(String firstName, String lastName);

    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r IN :roles")
    List<User> findByRolesIn(@Param("roles") Collection<String> roles);
}
package heritage.africca.accel_connect.service;

import heritage.africca.accel_connect.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

public interface UserService {

    User getOrCreateUserFromJwt(Jwt jwt);

    User createUserFromJwt(Jwt jwt, String keycloakId);

    User getCurrentUser(Authentication authentication);

    Optional<User> findByKeycloakId(String keycloakId);

    Optional<User> findByUsername(String username);

    User updateUser(Long userId, User updatedUser);

    Iterable<User> findAll();
}
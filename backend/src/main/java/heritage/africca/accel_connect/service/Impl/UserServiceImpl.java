package heritage.africca.accel_connect.service.Impl;

import heritage.africca.accel_connect.entity.EmployeeDetails;
import heritage.africca.accel_connect.entity.User;
import heritage.africca.accel_connect.repository.EmployeeDetailsRepository;
import heritage.africca.accel_connect.repository.UserRepository;
import heritage.africca.accel_connect.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final EmployeeDetailsRepository employeeDetailsRepository;

    /**
     * Récupère ou crée un utilisateur à partir du JWT Keycloak
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public User getOrCreateUserFromJwt(Jwt jwt) {
        String keycloakId = jwt.getSubject();

        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseGet(() -> createUserFromJwt(jwt, keycloakId));

        user.setRoles(extractRolesFromJwt(jwt));
        userRepository.save(user);

        if (employeeDetailsRepository.findByUserId(user.getId()).isEmpty()) {
            EmployeeDetails stub = new EmployeeDetails();
            stub.setUser(user);
            employeeDetailsRepository.save(stub);
            log.info("✅ EmployeeDetails stub créé pour: {}", user.getUsername());
        }

        return user;
    }

    /**
     * Crée un nouvel utilisateur à partir des claims JWT
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public User createUserFromJwt(Jwt jwt, String keycloakId) {
        User user = new User();
        user.setKeycloakId(keycloakId);
        user.setUsername(jwt.getClaim("preferred_username"));
        user.setEmail(jwt.getClaim("email"));
        user.setFirstName(jwt.getClaim("given_name"));
        user.setLastName(jwt.getClaim("family_name"));

        User savedUser = userRepository.save(user);
        log.info("✅ Nouvel utilisateur créé: {} (Keycloak ID: {})", savedUser.getUsername(), keycloakId);

        return savedUser;
    }

    /**
     * Récupère l'utilisateur actuellement authentifié
     * REMOVED readOnly to allow user creation
     */
    @Override
    @Transactional
    public User getCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
            throw new RuntimeException("Utilisateur non authentifié");
        }

        Jwt jwt = (Jwt) authentication.getPrincipal();
        return getOrCreateUserFromJwt(jwt);
    }

    @SuppressWarnings("unchecked")
    private Set<String> extractRolesFromJwt(Jwt jwt) {
        Set<String> roles = new HashSet<>();

        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof Collection) {
            ((Collection<String>) realmAccess.get("roles")).forEach(r -> roles.add(r.toUpperCase()));
        }

        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess != null) {
            resourceAccess.values().forEach(resource -> {
                if (resource instanceof Map) {
                    Object r = ((Map<?, ?>) resource).get("roles");
                    if (r instanceof Collection) {
                        ((Collection<String>) r).forEach(role -> roles.add(role.toUpperCase()));
                    }
                }
            });
        }

        return roles;
    }

    /**
     * Récupère un utilisateur par son ID Keycloak
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByKeycloakId(String keycloakId) {
        return userRepository.findByKeycloakId(keycloakId);
    }

    /**
     * Récupère un utilisateur par son username
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Met à jour les informations d'un utilisateur
     */
    @Override
    @Transactional
    public User updateUser(Long userId, User updatedUser) {
        return userRepository.findById(userId)
                .map(user -> {
                    if (updatedUser.getFirstName() != null) {
                        user.setFirstName(updatedUser.getFirstName());
                    }
                    if (updatedUser.getLastName() != null) {
                        user.setLastName(updatedUser.getLastName());
                    }
                    return userRepository.save(user);
                })
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + userId));
    }

    /**
     * Récupère tous les utilisateurs (pour l'admin)
     */
    @Override
    @Transactional(readOnly = true)
    public Iterable<User> findAll() {
        return userRepository.findAll();
    }
}
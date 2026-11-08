package heritage.africca.accel_connect.controller;

import heritage.africca.accel_connect.entity.Team;
import heritage.africca.accel_connect.entity.User;
import heritage.africca.accel_connect.repository.TeamRepository;
import heritage.africca.accel_connect.repository.UserRepository;
import heritage.africca.accel_connect.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/users")
@CrossOrigin("*")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;

    /**
     * Récupère les informations de l'utilisateur connecté
     */
    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser(Authentication authentication) {
        User user = userService.getCurrentUser(authentication);
        return ResponseEntity.ok(user);
    }

    /**
     * Met à jour le profil de l'utilisateur connecté
     */
    @PutMapping("/me")
    public ResponseEntity<User> updateCurrentUser(
            Authentication authentication,
            @RequestBody User updatedUser) {

        User currentUser = userService.getCurrentUser(authentication);
        User updated = userService.updateUser(currentUser.getId(), updatedUser);
        return ResponseEntity.ok(updated);
    }

    /**
     * Récupère un utilisateur par son username
     */
    @GetMapping("/username/{username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
        return userService.findByUsername(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ========== GESTION DES STAGIAIRES (ADMIN) ==========

    /**
     * Liste tous les utilisateurs
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR_MANAGER')")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> result = users.stream().map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("firstName", u.getFirstName());
            map.put("lastName", u.getLastName());
            map.put("username", u.getUsername());
            map.put("email",u.getEmail());
            map.put("intern", Boolean.TRUE.equals(u.getIntern()));
            map.put("subsidyAmount", Boolean.TRUE.equals(u.getIntern()) ? 2000 : 1000);
            return map;
        }).toList();
        return ResponseEntity.ok(result);
    }

    /**
     * Liste les stagiaires
     */
    @GetMapping("/interns")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getInterns() {
        List<User> interns = userRepository.findByInternTrue();
        List<Map<String, Object>> result = interns.stream().map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("firstName", u.getFirstName());
            map.put("lastName", u.getLastName());
            map.put("username", u.getUsername());
            map.put("subsidyAmount", 2000);
            return map;
        }).toList();
        return ResponseEntity.ok(result);
    }

    /**
     * Marquer un utilisateur comme stagiaire (subvention 2000 FCFA)
     */
    @PutMapping("/{userId}/set-intern")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> setIntern(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        user.setIntern(true);
        userRepository.save(user);

        log.info("Utilisateur {} {} marqué comme stagiaire (subvention 2000 FCFA)",
                user.getFirstName(), user.getLastName());

        Map<String, Object> response = new HashMap<>();
        response.put("message", user.getFirstName() + " " + user.getLastName() + " est maintenant stagiaire (subvention 2000 FCFA)");
        response.put("userId", userId);
        response.put("intern", true);
        response.put("subsidyAmount", 2000);
        return ResponseEntity.ok(response);
    }

    /**
     * Retirer le statut stagiaire (revient à subvention 1000 FCFA)
     */
    @PutMapping("/{userId}/remove-intern")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> removeIntern(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        user.setIntern(false);
        userRepository.save(user);

        log.info("Utilisateur {} {} retiré du statut stagiaire (subvention 1000 FCFA)",
                user.getFirstName(), user.getLastName());

        Map<String, Object> response = new HashMap<>();
        response.put("message", user.getFirstName() + " " + user.getLastName() + " n'est plus stagiaire (subvention 1000 FCFA)");
        response.put("userId", userId);
        response.put("intern", false);
        response.put("subsidyAmount", 1000);
        return ResponseEntity.ok(response);
    }

    /**
     * Liste tous les utilisateurs comme approbateurs potentiels (accessible à tous les employés)
     */
    @GetMapping("/colleagues")
    public ResponseEntity<List<Map<String, Object>>> getColleagues(Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);

        return teamRepository.findByMember(currentUser)
            .map(team -> {
                User manager = team.getManager();
                Map<String, Object> map = new HashMap<>();
                map.put("id", manager.getId());
                map.put("firstName", manager.getFirstName());
                map.put("lastName", manager.getLastName());
                map.put("email", manager.getEmail());
                return ResponseEntity.ok(List.<Map<String, Object>>of(map));
            })
            .orElseGet(() -> {
                List<User> managers = userRepository.findByRolesIn(List.of("MANAGER", "HR_MANAGER"));
                List<Map<String, Object>> result = managers.stream().map(u -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", u.getId());
                    m.put("firstName", u.getFirstName());
                    m.put("lastName", u.getLastName());
                    m.put("email", u.getEmail());
                    return m;
                }).toList();
                return ResponseEntity.ok(result);
            });
    }

    /**
     * Initialisation batch: marquer plusieurs utilisateurs comme stagiaires par nom
     * Body: { "names": ["Ibrahima Camara", "Khalifa Mouhamed Toure", ...] }
     */
    @PostMapping("/set-interns-batch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> setInternsBatch(@RequestBody Map<String, List<String>> body) {
        List<String> names = body.get("names");
        if (names == null || names.isEmpty()) {
            throw new RuntimeException("La liste des noms est vide");
        }

        List<String> found = new ArrayList<>();
        List<String> notFound = new ArrayList<>();

        for (String fullName : names) {
            String[] parts = fullName.trim().split("\\s+", 2);
            if (parts.length < 2) {
                notFound.add(fullName + " (format invalide, attendu: 'Prenom Nom')");
                continue;
            }

            String firstName = parts[0];
            String lastName = parts[1];

            Optional<User> userOpt = userRepository.findByFirstNameIgnoreCaseAndLastNameIgnoreCase(firstName, lastName);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setIntern(true);
                userRepository.save(user);
                found.add(user.getFirstName() + " " + user.getLastName());
                log.info("Stagiaire ajouté: {} {}", user.getFirstName(), user.getLastName());
            } else {
                notFound.add(fullName);
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("stagiairesAjoutes", found);
        response.put("nonTrouves", notFound);
        response.put("totalAjoutes", found.size());
        response.put("totalNonTrouves", notFound.size());
        return ResponseEntity.ok(response);
    }
}
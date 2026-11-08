package heritage.africca.accel_connect.controller;

import heritage.africca.accel_connect.dto.EmployeeSkillDto;
import heritage.africca.accel_connect.entity.User;
import heritage.africca.accel_connect.exception.ResourceNotFoundException;
import heritage.africca.accel_connect.repository.EmployeeDetailsRepository;
import heritage.africca.accel_connect.service.EmployeeSkillService;
import heritage.africca.accel_connect.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Employee self-service: manage own skills.
 * Resolves the employee ID from the authenticated user's JWT — no path parameter needed.
 */
@RestController
@RequestMapping("/api/v1/my/skills")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class MySkillController {

    private final EmployeeSkillService employeeSkillService;
    private final UserService userService;
    private final EmployeeDetailsRepository employeeDetailsRepository;

    private Long resolveOwnEmployeeId(Authentication auth) {
        User user = userService.getCurrentUser(auth);
        return employeeDetailsRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vos informations personnelles ne sont pas encore configurées. Contactez votre responsable RH."))
                .getId();
    }

    @GetMapping
    public ResponseEntity<List<EmployeeSkillDto>> getOwnSkills(Authentication auth) {
        return ResponseEntity.ok(employeeSkillService.getSkillsForEmployee(resolveOwnEmployeeId(auth)));
    }

    @PostMapping
    public ResponseEntity<EmployeeSkillDto> addOwnSkill(
            Authentication auth,
            @Valid @RequestBody EmployeeSkillDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(employeeSkillService.addSkillToEmployee(resolveOwnEmployeeId(auth), dto));
    }

    @PutMapping("/{skillId}")
    public ResponseEntity<EmployeeSkillDto> updateOwnSkill(
            Authentication auth,
            @PathVariable Integer skillId,
            @Valid @RequestBody EmployeeSkillDto dto) {
        return ResponseEntity.ok(employeeSkillService.updateEmployeeSkill(resolveOwnEmployeeId(auth), skillId, dto));
    }

    @DeleteMapping("/{skillId}")
    public ResponseEntity<Void> deleteOwnSkill(Authentication auth, @PathVariable Integer skillId) {
        employeeSkillService.removeEmployeeSkill(resolveOwnEmployeeId(auth), skillId);
        return ResponseEntity.noContent().build();
    }
}

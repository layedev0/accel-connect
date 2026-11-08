package heritage.africca.accel_connect.controller;

import heritage.africca.accel_connect.dto.EmployeeDependentDto;
import heritage.africca.accel_connect.dto.EmployeeDependentSummaryDto;
import heritage.africca.accel_connect.entity.User;
import heritage.africca.accel_connect.exception.ResourceNotFoundException;
import heritage.africca.accel_connect.repository.EmployeeDetailsRepository;
import heritage.africca.accel_connect.service.EmployeeDependentService;
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
 * Employee self-service: manage own dependents.
 * Resolves the employee ID from the authenticated user's JWT — no path parameter needed.
 */
@RestController
@RequestMapping("/api/v1/my/dependents")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class MyDependentController {

    private final EmployeeDependentService dependentService;
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
    public ResponseEntity<List<EmployeeDependentSummaryDto>> getOwnDependents(Authentication auth) {
        return ResponseEntity.ok(dependentService.getAllDependents(resolveOwnEmployeeId(auth)));
    }

    @PostMapping
    public ResponseEntity<EmployeeDependentDto> addOwnDependent(
            Authentication auth,
            @Valid @RequestBody EmployeeDependentDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dependentService.createDependent(resolveOwnEmployeeId(auth), dto));
    }

    @PutMapping("/{seqNo}")
    public ResponseEntity<EmployeeDependentDto> updateOwnDependent(
            Authentication auth,
            @PathVariable Integer seqNo,
            @Valid @RequestBody EmployeeDependentDto dto) {
        return ResponseEntity.ok(dependentService.updateDependent(resolveOwnEmployeeId(auth), seqNo, dto));
    }

    @DeleteMapping("/{seqNo}")
    public ResponseEntity<Void> deleteOwnDependent(Authentication auth, @PathVariable Integer seqNo) {
        dependentService.deleteDependent(resolveOwnEmployeeId(auth), seqNo);
        return ResponseEntity.noContent().build();
    }
}

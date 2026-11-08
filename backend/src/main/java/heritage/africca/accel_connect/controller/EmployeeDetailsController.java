package heritage.africca.accel_connect.controller;

import heritage.africca.accel_connect.dto.EmployeInfoPersonnelDTO;
import heritage.africca.accel_connect.dto.EmployeeDetailsRequest;
import heritage.africca.accel_connect.dto.EmployeeProfileDTO;
import heritage.africca.accel_connect.dto.UpcomingBirthdayDTO;
import heritage.africca.accel_connect.entity.EmployeeDetails;
import heritage.africca.accel_connect.entity.User;
import heritage.africca.accel_connect.service.EmployeeDetailsService;
import heritage.africca.accel_connect.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/employee-details")
@CrossOrigin("*")
@RequiredArgsConstructor
public class EmployeeDetailsController {

    private final EmployeeDetailsService employeeDetailsService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<EmployeeDetails>> findAll() {
        return ResponseEntity.ok(employeeDetailsService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeDetails> getById(@PathVariable Long id) {
        return ResponseEntity.ok(employeeDetailsService.getById(id));
    }

    /**
     * Employees whose birthday falls within the next {@code days} days (default 30),
     * ordered by the soonest first. Visible to any authenticated employee.
     */
    @GetMapping("/birthdays/upcoming")
    public ResponseEntity<List<UpcomingBirthdayDTO>> getUpcomingBirthdays(
            @RequestParam(name = "days", defaultValue = "30") int days) {
        return ResponseEntity.ok(employeeDetailsService.getUpcomingBirthdays(days));
    }

    @GetMapping("/by-user/{userId}")
    public ResponseEntity<EmployeInfoPersonnelDTO> getEmployeInfoPersonnelByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(employeeDetailsService.getEmployeInfoPersonnelByUserId(userId));
    }

    /**
     * Returns employee profile for display in the RH employee-profile section.
     * Combines User and EmployeeDetails (when available).
     */
    @GetMapping("/profile/by-user/{userId}")
    @PreAuthorize("hasRole('HR_MANAGER')")
    public ResponseEntity<EmployeeProfileDTO> getEmployeeProfileByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(employeeDetailsService.getEmployeeProfileByUserId(userId));
    }

    /**
     * Returns employee personnel info if it exists, or 200 with null when not found.
     * Use this when the UI needs to load existing data OR show an empty form to complete.
     */
    @GetMapping("/by-user/{userId}/optional")
    public ResponseEntity<EmployeInfoPersonnelDTO> getEmployeInfoPersonnelByUserIdOptional(@PathVariable Long userId) {
        return ResponseEntity.ok(
                employeeDetailsService.getEmployeInfoPersonnelByUserIdOptional(userId).orElse(null));
    }

    @PostMapping
    @PreAuthorize("hasRole('HR_MANAGER')")
    public ResponseEntity<Void> create(@Valid @RequestBody EmployeeDetailsRequest request) {
        employeeDetailsService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Allows any authenticated employee to save their own personal info.
     * The userId is always forced to the authenticated user's DB id,
     * so an employee can only ever modify their own record.
     */
    @PutMapping("/save-own")
    public ResponseEntity<Void> saveOwn(@Valid @RequestBody EmployeeDetailsRequest request,
                                        Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        request.setUserId(currentUser.getId());
        EmployeeDetails result = employeeDetailsService.createOrUpdate(request);
        boolean wasUpdate = request.getId() != null && result.getId().equals(request.getId());
        return wasUpdate ? ResponseEntity.ok().build() : ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Create or update employee in one call.
     * If request.id is provided and exists, updates; otherwise creates new.
     */
    @PutMapping("/save")
    @PreAuthorize("hasRole('HR_MANAGER')")
    public ResponseEntity<Void> createOrUpdate(@Valid @RequestBody EmployeeDetailsRequest request) {
        EmployeeDetails result = employeeDetailsService.createOrUpdate(request);
        boolean wasUpdate = request.getId() != null && result.getId().equals(request.getId());
        return wasUpdate ? ResponseEntity.ok().build() : ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('HR_MANAGER')")
    public ResponseEntity<Void> update(@PathVariable Long id,
                                       @Valid @RequestBody EmployeeDetailsRequest request) {
        employeeDetailsService.update(id, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('HR_MANAGER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        employeeDetailsService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

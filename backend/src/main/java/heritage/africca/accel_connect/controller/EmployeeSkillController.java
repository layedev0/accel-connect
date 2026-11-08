package heritage.africca.accel_connect.controller;

import heritage.africca.accel_connect.dto.EmployeeSkillDto;
import heritage.africca.accel_connect.service.EmployeeSkillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/employees/{employeeId}/skills")
@RequiredArgsConstructor
public class EmployeeSkillController {

    private final EmployeeSkillService employeeSkillService;

    @GetMapping
    @PreAuthorize("hasRole('HR_MANAGER')")
    public ResponseEntity<List<EmployeeSkillDto>> getSkillsForEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(employeeSkillService.getSkillsForEmployee(employeeId));
    }

    @GetMapping("/{skillId}")
    @PreAuthorize("hasRole('HR_MANAGER')")
    public ResponseEntity<EmployeeSkillDto> getEmployeeSkill(@PathVariable Long employeeId,
                                                             @PathVariable Integer skillId) {
        return ResponseEntity.ok(employeeSkillService.getEmployeeSkill(employeeId, skillId));
    }

    @PostMapping
    @PreAuthorize("hasRole('HR_MANAGER')")
    public ResponseEntity<EmployeeSkillDto> addSkillToEmployee(@PathVariable Long employeeId,
                                                               @Valid @RequestBody EmployeeSkillDto dto) {
        EmployeeSkillDto created = employeeSkillService.addSkillToEmployee(employeeId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{skillId}")
    @PreAuthorize("hasRole('HR_MANAGER')")
    public ResponseEntity<EmployeeSkillDto> updateEmployeeSkill(@PathVariable Long employeeId,
                                                                @PathVariable Integer skillId,
                                                                @Valid @RequestBody EmployeeSkillDto dto) {
        return ResponseEntity.ok(employeeSkillService.updateEmployeeSkill(employeeId, skillId, dto));
    }

    @DeleteMapping("/{skillId}")
    @PreAuthorize("hasRole('HR_MANAGER')")
    public ResponseEntity<Void> removeEmployeeSkill(@PathVariable Long employeeId,
                                                    @PathVariable Integer skillId) {
        employeeSkillService.removeEmployeeSkill(employeeId, skillId);
        return ResponseEntity.noContent().build();
    }
}


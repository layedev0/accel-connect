package heritage.africca.accel_connect.controller;

import heritage.africca.accel_connect.dto.EmployeeDependentDto;
import heritage.africca.accel_connect.dto.EmployeeDependentSummaryDto;
import heritage.africca.accel_connect.service.EmployeeDependentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/employees/{employeeId}/dependents")
@RequiredArgsConstructor
@Tag(name = "Employee Dependents Management", description = "Endpoints for managing employee dependents")
public class EmployeeDependentController {

    private final EmployeeDependentService dependentService;



    @PostMapping
    @PreAuthorize("hasRole('HR_MANAGER')")
    @Operation(summary = "Add a dependent for an employee")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Dependent created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Employee not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Dependent already exists")
    })
    public ResponseEntity<EmployeeDependentDto> createDependent(
            @PathVariable Long employeeId,
            @Valid @RequestBody EmployeeDependentDto dependentDto) {
        EmployeeDependentDto createdDependent = dependentService.createDependent(employeeId, dependentDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(createdDependent);
    }

    @PutMapping("/{seqNo}")
    @PreAuthorize("hasRole('HR_MANAGER')")
    @Operation(summary = "Update a dependent")
    public ResponseEntity<EmployeeDependentDto> updateDependent(
            @PathVariable Long employeeId,
            @PathVariable Integer seqNo,
            @Valid @RequestBody EmployeeDependentDto dependentDto) {
        EmployeeDependentDto updatedDependent = dependentService.updateDependent(employeeId, seqNo, dependentDto);
        return ResponseEntity.ok(updatedDependent);
    }

    @DeleteMapping("/{seqNo}")
    @PreAuthorize("hasRole('HR_MANAGER')")
    @Operation(summary = "Delete a dependent")
    public ResponseEntity<String> deleteDependent(
            @PathVariable Long employeeId,
            @PathVariable Integer seqNo) {
        dependentService.deleteDependent(employeeId, seqNo);
        return ResponseEntity.ok("Dependent deleted successfully");
    }



    @GetMapping("/{seqNo}")
    @PreAuthorize("hasRole('HR_MANAGER')")
    @Operation(summary = "Get a dependent by sequence number")
    public ResponseEntity<EmployeeDependentDto> getDependent(
            @PathVariable Long employeeId,
            @PathVariable Integer seqNo) {
        EmployeeDependentDto dependent = dependentService.getDependent(employeeId, seqNo);
        return ResponseEntity.ok(dependent);
    }

    @GetMapping
    @PreAuthorize("hasRole('HR_MANAGER')")
    @Operation(summary = "Get all dependents for an employee")
    public ResponseEntity<List<EmployeeDependentSummaryDto>> getAllDependents(
            @PathVariable Long employeeId) {
        List<EmployeeDependentSummaryDto> dependents = dependentService.getAllDependents(employeeId);
        return ResponseEntity.ok(dependents);
    }

    // ========== Statistics Endpoints ==========

    @GetMapping("/count")
    @PreAuthorize("hasRole('HR_MANAGER')")
    @Operation(summary = "Get dependent count for an employee")
    public ResponseEntity<Long> getDependentCount(@PathVariable Long employeeId) {
        long count = dependentService.getDependentCount(employeeId);
        return ResponseEntity.ok(count);
    }



    // ========== Search Endpoints ==========

    @GetMapping("/search")
    @PreAuthorize("hasRole('HR_MANAGER')")
    @Operation(summary = "Search dependents by name or relationship")
    public ResponseEntity<List<EmployeeDependentSummaryDto>> searchDependents(
            @PathVariable Long employeeId,
            @RequestParam String term) {
        List<EmployeeDependentSummaryDto> dependents = dependentService.searchDependents(employeeId, term);
        return ResponseEntity.ok(dependents);
    }
}
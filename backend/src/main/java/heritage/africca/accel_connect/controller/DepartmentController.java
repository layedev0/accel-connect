package heritage.africca.accel_connect.controller;

import heritage.africca.accel_connect.dto.DepartmentRequest;
import heritage.africca.accel_connect.entity.Department;
import heritage.africca.accel_connect.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/departments")
@CrossOrigin("*")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    public ResponseEntity<List<Department>> findAll() {
        return ResponseEntity.ok(departmentService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Department> getById(@PathVariable Long id) {
        return ResponseEntity.ok(departmentService.getDepartmentById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('HR_MANAGER')")
    public ResponseEntity<Department> create(@Valid @RequestBody DepartmentRequest request) {
        Department created = departmentService.createDepartment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('HR_MANAGER')")
    public ResponseEntity<Department> update(@PathVariable Long id, @Valid @RequestBody DepartmentRequest request) {
        return ResponseEntity.ok(departmentService.updateDepartment(id, request));
    }


    @DeleteMapping(params = "id")
    @PreAuthorize("hasRole('HR_MANAGER')")
    public ResponseEntity<Void> deleteByQueryParam(@RequestParam Long id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.noContent().build();
    }
}

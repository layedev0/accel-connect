package heritage.africca.accel_connect.controller;

import heritage.africca.accel_connect.dto.JobCategoryRequest;
import heritage.africca.accel_connect.entity.JobCategory;
import heritage.africca.accel_connect.service.JobCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/job-categories")
@CrossOrigin("*")
@RequiredArgsConstructor
public class JobCategoryController {

    private final JobCategoryService jobCategoryService;

    @GetMapping
    public ResponseEntity<List<JobCategory>> findAll() {
        return ResponseEntity.ok(jobCategoryService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobCategory> getById(@PathVariable Long id) {
        return ResponseEntity.ok(jobCategoryService.getJobCategoryById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('HR_MANAGER')")
    public ResponseEntity<JobCategory> create(@Valid @RequestBody JobCategoryRequest request) {
        JobCategory created = jobCategoryService.createJobCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('HR_MANAGER')")
    public ResponseEntity<JobCategory> update(@PathVariable Long id, @Valid @RequestBody JobCategoryRequest request) {
        return ResponseEntity.ok(jobCategoryService.updateJobCategory(id, request));
    }


    @DeleteMapping(params = "id")
    @PreAuthorize("hasRole('HR_MANAGER')")
    public ResponseEntity<Void> deleteByQueryParam(@RequestParam Long id) {
        jobCategoryService.deleteJobCategory(id);
        return ResponseEntity.noContent().build();
    }
}

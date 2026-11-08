package heritage.africca.accel_connect.controller;

import heritage.africca.accel_connect.dto.JobTitleRequest;
import heritage.africca.accel_connect.entity.JobTitle;
import heritage.africca.accel_connect.service.JobTitleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/job-titles")
@CrossOrigin("*")
@RequiredArgsConstructor
public class JobTitleController {

    private final JobTitleService jobTitleService;

    @GetMapping
    public ResponseEntity<List<JobTitle>> findAll() {
        return ResponseEntity.ok(jobTitleService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobTitle> getById(@PathVariable Long id) {
        return ResponseEntity.ok(jobTitleService.getJobTitleById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('HR_MANAGER')")
    public ResponseEntity<JobTitle> create(@Valid @RequestBody JobTitleRequest request) {
        JobTitle created = jobTitleService.createJobTitle(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('HR_MANAGER')")
    public ResponseEntity<JobTitle> update(@PathVariable Long id, @Valid @RequestBody JobTitleRequest request) {
        return ResponseEntity.ok(jobTitleService.updateJobTitle(id, request));
    }


    @DeleteMapping(params = "id")
    @PreAuthorize("hasRole('HR_MANAGER')")
    public ResponseEntity<Void> deleteByQueryParam(@RequestParam Long id) {
        jobTitleService.deleteJobTitle(id);
        return ResponseEntity.noContent().build();
    }
}

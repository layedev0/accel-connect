package heritage.africca.accel_connect.controller;

import heritage.africca.accel_connect.dto.SkillRequest;
import heritage.africca.accel_connect.entity.Skill;
import heritage.africca.accel_connect.service.SkillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/skills")
@CrossOrigin("*")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;

    @GetMapping
    public ResponseEntity<List<Skill>> findAll() {
        return ResponseEntity.ok(skillService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Skill> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(skillService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('HR_MANAGER')")
    public ResponseEntity<Skill> create(@Valid @RequestBody SkillRequest request) {
        Skill created = skillService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('HR_MANAGER')")
    public ResponseEntity<Skill> update(@PathVariable Integer id, @Valid @RequestBody SkillRequest request) {
        return ResponseEntity.ok(skillService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('HR_MANAGER')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        skillService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

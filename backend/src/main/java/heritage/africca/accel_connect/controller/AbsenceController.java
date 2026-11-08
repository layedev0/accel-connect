package heritage.africca.accel_connect.controller;

import heritage.africca.accel_connect.dto.AbsenceDecisionRequest;
import heritage.africca.accel_connect.dto.AbsenceRequestDto;
import heritage.africca.accel_connect.dto.AbsenceSummaryDto;
import heritage.africca.accel_connect.service.AbsenceService;
import heritage.africca.accel_connect.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/absences")
@CrossOrigin("*")
@RequiredArgsConstructor
public class AbsenceController {

    private final AbsenceService absenceService;
    private final UserService userService;

    @PostMapping("/users/{userId}/submit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AbsenceSummaryDto> submitAbsence(
            @PathVariable Long userId,
            @RequestBody @Valid AbsenceRequestDto request) {
        AbsenceSummaryDto response = absenceService.submitAbsence(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/users/{userId}/requests")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AbsenceSummaryDto>> getUserAbsences(@PathVariable Long userId) {
        return ResponseEntity.ok(absenceService.getUserAbsences(userId));
    }

    @GetMapping("/requests/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AbsenceSummaryDto> getAbsenceById(@PathVariable Integer id) {
        return ResponseEntity.ok(absenceService.getAbsenceById(id));
    }

    @GetMapping("/pending-requests")
    @PreAuthorize("hasRole('HR_MANAGER') or hasRole('MANAGER')")
    public ResponseEntity<List<AbsenceSummaryDto>> getPendingAbsences(Authentication authentication) {
        var currentUser = userService.getCurrentUser(authentication);
        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        return ResponseEntity.ok(absenceService.getPendingAbsences(currentUser.getId(), roles));
    }

    @GetMapping("/approved-requests")
    @PreAuthorize("hasRole('HR_MANAGER') or hasRole('MANAGER')")
    public ResponseEntity<List<AbsenceSummaryDto>> getApprovedAbsences(Authentication authentication) {
        var currentUser = userService.getCurrentUser(authentication);
        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        return ResponseEntity.ok(absenceService.getApprovedAbsences(currentUser.getId(), roles));
    }

    @GetMapping("/rejected-requests")
    @PreAuthorize("hasRole('HR_MANAGER') or hasRole('MANAGER')")
    public ResponseEntity<List<AbsenceSummaryDto>> getRejectedAbsences(Authentication authentication) {
        var currentUser = userService.getCurrentUser(authentication);
        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        return ResponseEntity.ok(absenceService.getRejectedAbsences(currentUser.getId(), roles));
    }

    @PutMapping("/requests/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AbsenceSummaryDto> updateAbsence(
            @PathVariable Integer id,
            @RequestParam Long userId,
            @RequestBody @Valid AbsenceRequestDto request) {
        return ResponseEntity.ok(absenceService.updateAbsence(id, userId, request));
    }

    @PatchMapping("/requests/{id}/approve")
    @PreAuthorize("hasRole('HR_MANAGER') or hasRole('MANAGER')")
    public ResponseEntity<AbsenceSummaryDto> approveAbsence(
            @PathVariable Integer id,
            @RequestBody(required = false) AbsenceDecisionRequest request,
            Authentication authentication) {
        var currentUser = userService.getCurrentUser(authentication);
        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        AbsenceDecisionRequest decision = request != null ? request : new AbsenceDecisionRequest();
        if (decision.getDecidedByUserId() == null) {
            decision.setDecidedByUserId(currentUser.getId());
        }
        return ResponseEntity.ok(absenceService.approveAbsence(id, decision, currentUser.getId(), roles));
    }

    @PatchMapping("/requests/{id}/reject")
    @PreAuthorize("hasRole('HR_MANAGER') or hasRole('MANAGER')")
    public ResponseEntity<AbsenceSummaryDto> rejectAbsence(
            @PathVariable Integer id,
            @RequestBody(required = false) AbsenceDecisionRequest request,
            Authentication authentication) {
        var currentUser = userService.getCurrentUser(authentication);
        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        AbsenceDecisionRequest decision = request != null ? request : new AbsenceDecisionRequest();
        if (decision.getDecidedByUserId() == null) {
            decision.setDecidedByUserId(currentUser.getId());
        }
        return ResponseEntity.ok(absenceService.rejectAbsence(id, decision, currentUser.getId(), roles));
    }

    @PatchMapping("/requests/{id}/cancel-approved")
    @PreAuthorize("hasRole('HR_MANAGER') or hasRole('MANAGER')")
    public ResponseEntity<AbsenceSummaryDto> cancelApprovedAbsence(
            @PathVariable Integer id,
            @RequestBody(required = false) AbsenceDecisionRequest request,
            Authentication authentication) {
        var currentUser = userService.getCurrentUser(authentication);
        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        AbsenceDecisionRequest decision = request != null ? request : new AbsenceDecisionRequest();
        if (decision.getDecidedByUserId() == null) {
            decision.setDecidedByUserId(currentUser.getId());
        }
        return ResponseEntity.ok(absenceService.cancelApprovedAbsence(id, decision, currentUser.getId(), roles));
    }

    @PatchMapping("/requests/{id}/cancel-rejection")
    @PreAuthorize("hasRole('HR_MANAGER') or hasRole('MANAGER')")
    public ResponseEntity<AbsenceSummaryDto> cancelRejectedAbsence(
            @PathVariable Integer id,
            @RequestBody(required = false) AbsenceDecisionRequest request,
            Authentication authentication) {
        var currentUser = userService.getCurrentUser(authentication);
        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        AbsenceDecisionRequest decision = request != null ? request : new AbsenceDecisionRequest();
        if (decision.getDecidedByUserId() == null) {
            decision.setDecidedByUserId(currentUser.getId());
        }
        return ResponseEntity.ok(absenceService.cancelRejectedAbsence(id, decision, currentUser.getId(), roles));
    }

    @DeleteMapping("/requests/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteAbsence(
            @PathVariable Integer id,
            @RequestParam Long userId) {
        absenceService.deleteAbsence(id, userId);
        return ResponseEntity.noContent().build();
    }
}

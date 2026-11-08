package heritage.africca.accel_connect.controller;

import heritage.africca.accel_connect.dto.UserLeaveBalanceDto;
import heritage.africca.accel_connect.service.LeaveBalanceService;
import heritage.africca.accel_connect.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/leave-balances")
public class LeaveBalanceController {

    private final LeaveBalanceService leaveBalanceService;
    private final UserService userService;

    public LeaveBalanceController(LeaveBalanceService leaveBalanceService, UserService userService) {
        this.leaveBalanceService = leaveBalanceService;
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasRole('HR_MANAGER')")
    public ResponseEntity<List<UserLeaveBalanceDto>> listAll() {
        return ResponseEntity.ok(leaveBalanceService.listAllBalances());
    }

    @GetMapping("/me")
    public ResponseEntity<UserLeaveBalanceDto> getMyBalance(Authentication authentication) {
        var currentUser = userService.getCurrentUser(authentication);
        UserLeaveBalanceDto dto = leaveBalanceService.getBalanceForUser(currentUser.getId());
        if (dto == null) {
            dto = UserLeaveBalanceDto.builder()
                    .userId(currentUser.getId())
                    .userName(currentUser.getFirstName() + " " + currentUser.getLastName())
                    .userEmail(currentUser.getEmail())
                    .balanceDays(BigDecimal.ZERO)
                    .leaveDue(BigDecimal.ZERO)
                    .leaveTaken(BigDecimal.ZERO)
                    .leaveBalance(BigDecimal.ZERO)
                    .build();
        }
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('HR_MANAGER')")
    public ResponseEntity<UserLeaveBalanceDto> getByUser(@PathVariable Long userId) {
        UserLeaveBalanceDto dto = leaveBalanceService.getBalanceForUser(userId);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }

    @PutMapping("/{userId}/add-balance")
    @PreAuthorize("hasRole('HR_MANAGER')")
    public ResponseEntity<Map<String, Object>> addBalance(@PathVariable Long userId, @RequestParam BigDecimal days) {
        leaveBalanceService.addBalanceForUser(userId, days);
        return ResponseEntity.ok(Map.of("message", "Balance updated successfully", "userId", userId, "addedDays", days));
    }

    @PutMapping("/{userId}/deduct-balance")
    @PreAuthorize("hasRole('HR_MANAGER')")
    public ResponseEntity<Map<String, Object>> deductBalance(@PathVariable Long userId, @RequestParam BigDecimal days) {
        leaveBalanceService.deductBalanceForUser(userId, days);
        return ResponseEntity.ok(Map.of("message", "Balance deducted successfully", "userId", userId, "deductedDays", days));
    }

    @PutMapping("/{userId}/add-leave-due")
    @PreAuthorize("hasRole('HR_MANAGER')")
    public ResponseEntity<Map<String, Object>> addLeaveDue(@PathVariable Long userId, @RequestParam BigDecimal days) {
        leaveBalanceService.addLeaveDue(userId, days);
        return ResponseEntity.ok(Map.of("message", "Congés dû mis à jour", "userId", userId, "addedDays", days));
    }

    @PutMapping("/{userId}/deduct-leave-due")
    @PreAuthorize("hasRole('HR_MANAGER')")
    public ResponseEntity<Map<String, Object>> deductLeaveDue(@PathVariable Long userId, @RequestParam BigDecimal days) {
        leaveBalanceService.deductLeaveDue(userId, days);
        return ResponseEntity.ok(Map.of("message", "Congés dû mis à jour", "userId", userId, "deductedDays", days));
    }

    @PutMapping("/{userId}/add-leave-taken")
    @PreAuthorize("hasRole('HR_MANAGER')")
    public ResponseEntity<Map<String, Object>> addLeaveTaken(@PathVariable Long userId, @RequestParam BigDecimal days) {
        leaveBalanceService.addLeaveTaken(userId, days);
        return ResponseEntity.ok(Map.of("message", "Congés pris mis à jour", "userId", userId, "addedDays", days));
    }

    @PutMapping("/{userId}/deduct-leave-taken")
    @PreAuthorize("hasRole('HR_MANAGER')")
    public ResponseEntity<Map<String, Object>> deductLeaveTaken(@PathVariable Long userId, @RequestParam BigDecimal days) {
        leaveBalanceService.deductLeaveTaken(userId, days);
        return ResponseEntity.ok(Map.of("message", "Congés pris mis à jour", "userId", userId, "deductedDays", days));
    }
}



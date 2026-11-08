package heritage.africca.accel_connect.controller;

import heritage.africca.accel_connect.dto.NotificationDto;
import heritage.africca.accel_connect.service.NotificationService;
import heritage.africca.accel_connect.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@CrossOrigin("*")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NotificationDto>> getUnread(Authentication authentication) {
        Long userId = userService.getCurrentUser(authentication).getId();
        return ResponseEntity.ok(notificationService.getUnread(userId));
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markRead(@PathVariable Long id, Authentication authentication) {
        Long userId = userService.getCurrentUser(authentication).getId();
        notificationService.markRead(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/mark-all-read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markAllRead(Authentication authentication) {
        Long userId = userService.getCurrentUser(authentication).getId();
        notificationService.markAllRead(userId);
        return ResponseEntity.noContent().build();
    }
}

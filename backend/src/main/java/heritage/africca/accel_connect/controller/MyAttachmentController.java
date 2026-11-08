package heritage.africca.accel_connect.controller;

import heritage.africca.accel_connect.dto.EmployeeAttachmentDTO;
import heritage.africca.accel_connect.entity.EmployeeAttachment;
import heritage.africca.accel_connect.entity.User;
import heritage.africca.accel_connect.service.EmployeeAttachmentService;
import heritage.africca.accel_connect.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Employee self-service: view and download own documents.
 * Read-only — employees cannot upload or delete via this endpoint.
 */
@RestController
@RequestMapping("/api/v1/my/attachments")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class MyAttachmentController {

    private final EmployeeAttachmentService attachmentService;
    private final UserService userService;

    private Long resolveOwnUserId(Authentication auth) {
        User user = userService.getCurrentUser(auth);
        return user.getId();
    }

    @GetMapping
    public ResponseEntity<List<EmployeeAttachmentDTO>> listOwnAttachments(Authentication auth) {
        return ResponseEntity.ok(attachmentService.listAttachments(resolveOwnUserId(auth)));
    }

    @GetMapping("/{attachId}/download")
    public ResponseEntity<byte[]> downloadOwnAttachment(Authentication auth, @PathVariable Integer attachId) {
        Long userId = resolveOwnUserId(auth);
        EmployeeAttachment entity = attachmentService.download(userId, attachId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + entity.getFilename() + "\"")
                .contentType(MediaType.parseMediaType(entity.getFileType()))
                .body(entity.getAttachment());
    }
}

package heritage.africca.accel_connect.controller;

import heritage.africca.accel_connect.dto.EmployeeAttachmentDTO;
import heritage.africca.accel_connect.entity.EmployeeAttachment;
import heritage.africca.accel_connect.service.EmployeeAttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/employees/{userId}/attachments")
@RequiredArgsConstructor
@Tag(name = "Employee Attachments", description = "Endpoints for managing employee documents")
public class EmployeeAttachmentController {

    private final EmployeeAttachmentService attachmentService;

    @GetMapping
    @PreAuthorize("hasRole('HR_MANAGER')")
    @Operation(summary = "List all attachments for a user (metadata only)")
    public ResponseEntity<List<EmployeeAttachmentDTO>> list(@PathVariable Long userId) {
        return ResponseEntity.ok(attachmentService.listAttachments(userId));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('HR_MANAGER')")
    @Operation(summary = "Upload a document for a user")
    public ResponseEntity<EmployeeAttachmentDTO> upload(
            @PathVariable Long userId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("description") String description,
            @RequestParam("screen") String screen,
            Authentication authentication) throws IOException {
        EmployeeAttachmentDTO dto = attachmentService.upload(userId, file, description, screen, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/{attachId}/download")
    @PreAuthorize("hasRole('HR_MANAGER')")
    @Operation(summary = "Download a specific document")
    public ResponseEntity<byte[]> download(
            @PathVariable Long userId,
            @PathVariable Integer attachId) {
        EmployeeAttachment attachment = attachmentService.download(userId, attachId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(attachment.getFileType()));
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(attachment.getFilename()).build()
        );
        return ResponseEntity.ok().headers(headers).body(attachment.getAttachment());
    }

    @DeleteMapping("/{attachId}")
    @PreAuthorize("hasRole('HR_MANAGER')")
    @Operation(summary = "Delete a document")
    public ResponseEntity<Void> delete(
            @PathVariable Long userId,
            @PathVariable Integer attachId) {
        attachmentService.delete(userId, attachId);
        return ResponseEntity.noContent().build();
    }
}

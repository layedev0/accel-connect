package heritage.africca.accel_connect.dto;

import java.time.LocalDateTime;

/**
 * Response DTO for employee attachments — intentionally excludes the binary blob
 * so that listing endpoints don't transfer file data unnecessarily.
 */
public record EmployeeAttachmentDTO(
        Long userId,
        Integer attachId,
        String description,
        String filename,
        Integer size,
        String fileType,
        String screen,
        String attachedByName,
        LocalDateTime createdAt
) {}

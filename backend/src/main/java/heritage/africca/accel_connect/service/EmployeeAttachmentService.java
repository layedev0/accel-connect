package heritage.africca.accel_connect.service;

import heritage.africca.accel_connect.dto.EmployeeAttachmentDTO;
import heritage.africca.accel_connect.entity.EmployeeAttachment;
import heritage.africca.accel_connect.exception.ResourceNotFoundException;
import heritage.africca.accel_connect.exception.ValidationException;
import heritage.africca.accel_connect.repository.EmployeeAttachmentRepository;
import heritage.africca.accel_connect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeAttachmentService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024L; // 10 MB
    private static final List<String> ALLOWED_TYPES = List.of(
            MediaType.APPLICATION_PDF_VALUE,
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE
    );

    private final EmployeeAttachmentRepository attachmentRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    public List<EmployeeAttachmentDTO> listAttachments(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with ID: " + userId);
        }
        return attachmentRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public EmployeeAttachmentDTO upload(Long userId, MultipartFile file,
                                        String description, String screen,
                                        Authentication authentication) throws IOException {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with ID: " + userId);
        }

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier est vide.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Le fichier dépasse la taille maximale autorisée (10 Mo).");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Type de fichier non autorisé. Formats acceptés : PDF, JPEG, PNG.");
        }

        var currentUser = userService.getCurrentUser(authentication);

        Integer nextAttachId = attachmentRepository.findMaxAttachIdByUserId(userId)
                .map(max -> max + 1)
                .orElse(1);

        EmployeeAttachment attachment = new EmployeeAttachment();
        attachment.setUserId(userId);
        attachment.setAttachId(nextAttachId);
        attachment.setFilename(file.getOriginalFilename());
        attachment.setSize((int) file.getSize());
        attachment.setFileType(contentType);
        attachment.setDescription(description);
        attachment.setScreen(screen);
        attachment.setAttachedBy(currentUser.getId().intValue());
        attachment.setAttachedByName(currentUser.getFirstName() + " " + currentUser.getLastName());
        attachment.setAttachment(file.getBytes());

        EmployeeAttachment saved = attachmentRepository.save(attachment);
        log.info("Attachment {} uploaded for user {} by {}", nextAttachId, userId, currentUser.getUsername());
        return toDto(saved);
    }

    public EmployeeAttachment download(Long userId, Integer attachId) {
        return attachmentRepository.findByUserIdAndAttachId(userId, attachId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document introuvable pour l'utilisateur " + userId + " avec l'identifiant " + attachId));
    }

    public List<EmployeeAttachmentDTO> listLeaveRequestAttachments(Integer leaveRequestId) {
        String screen = "LEAVE_REQUEST_" + leaveRequestId;
        return attachmentRepository.findByScreen(screen)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public EmployeeAttachment downloadLeaveRequestAttachment(Integer leaveRequestId, Integer attachId) {
        String screen = "LEAVE_REQUEST_" + leaveRequestId;
        return attachmentRepository.findByScreenAndAttachId(screen, attachId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document introuvable pour la demande de congé " + leaveRequestId));
    }

    @Transactional
    public void delete(Long userId, Integer attachId) {
        if (attachmentRepository.findByUserIdAndAttachId(userId, attachId).isEmpty()) {
            throw new ResourceNotFoundException(
                    "Document introuvable pour l'utilisateur " + userId + " avec l'identifiant " + attachId);
        }
        attachmentRepository.deleteByUserIdAndAttachId(userId, attachId);
        log.info("Attachment {} deleted for user {}", attachId, userId);
    }

    @Transactional
    public void deleteLeaveRequestAttachment(Long userId, Integer leaveRequestId, Integer attachId) {
        String screen = "LEAVE_REQUEST_" + leaveRequestId;
        EmployeeAttachment attachment = attachmentRepository.findByScreenAndAttachId(screen, attachId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document introuvable pour la demande de congé " + leaveRequestId));
        if (!attachment.getUserId().equals(userId)) {
            throw new ValidationException("Vous ne pouvez supprimer que vos propres pièces jointes.");
        }
        attachmentRepository.deleteByUserIdAndAttachId(userId, attachId);
        log.info("Attachment {} deleted from leave request {} for user {}", attachId, leaveRequestId, userId);
    }

    @Transactional
    public EmployeeAttachmentDTO reassignToLeaveRequest(Long userId, Integer attachId, Integer newLeaveRequestId) {
        EmployeeAttachment attachment = attachmentRepository.findByUserIdAndAttachId(userId, attachId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document introuvable pour l'utilisateur " + userId + " avec l'identifiant " + attachId));
        if (attachment.getScreen() == null || !attachment.getScreen().startsWith("LEAVE_REQUEST_")) {
            throw new ValidationException("Cette pièce jointe n'est pas associée à une demande de congé.");
        }
        attachment.setScreen("LEAVE_REQUEST_" + newLeaveRequestId);
        EmployeeAttachment saved = attachmentRepository.save(attachment);
        log.info("Attachment {} reassigned to leave request {} for user {}", attachId, newLeaveRequestId, userId);
        return toDto(saved);
    }

    private EmployeeAttachmentDTO toDto(EmployeeAttachment a) {
        return new EmployeeAttachmentDTO(
                a.getUserId(),
                a.getAttachId(),
                a.getDescription(),
                a.getFilename(),
                a.getSize(),
                a.getFileType(),
                a.getScreen(),
                a.getAttachedByName(),
                a.getCreatedAt()
        );
    }
}

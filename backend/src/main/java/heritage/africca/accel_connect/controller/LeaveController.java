package heritage.africca.accel_connect.controller;

import heritage.africca.accel_connect.dto.LeaveApplicationResponseDto;
import heritage.africca.accel_connect.dto.LeaveBalanceInfoDto;
import heritage.africca.accel_connect.dto.LeaveDecisionRequest;
import heritage.africca.accel_connect.dto.LeaveDto;
import heritage.africca.accel_connect.dto.LeaveRequestSummaryDto;
import heritage.africca.accel_connect.dto.EmployeeAttachmentDTO;
import heritage.africca.accel_connect.entity.LeaveRequest;
import heritage.africca.accel_connect.service.LeaveService;
import heritage.africca.accel_connect.service.UserService;
import heritage.africca.accel_connect.service.EmployeeAttachmentService;
import heritage.africca.accel_connect.repository.EmployeeDetailsRepository;
import heritage.africca.accel_connect.exception.ValidationException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/leaves")
@CrossOrigin("*")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;
    private final UserService userService;
    private final EmployeeAttachmentService attachmentService;
    private final EmployeeDetailsRepository employeeDetailsRepository;

    @GetMapping("/leave-requests/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LeaveRequest> getLeaveRequest(
            @PathVariable Integer id,
            Authentication authentication) {
        LeaveRequest request = leaveService.getLeaveRequestById(id);
        var currentUser = userService.getCurrentUser(authentication);
        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        leaveService.validateLeaveRequestViewAccess(request, currentUser.getId(), roles);
        return ResponseEntity.ok(request);
    }

    @GetMapping("/users/{userId}/requests")
    public ResponseEntity<List<LeaveRequestSummaryDto>> getUserLeaveRequests(@PathVariable Long userId) {
        return ResponseEntity.ok(leaveService.getUserLeaveRequests(userId));
    }

    @GetMapping("/users/{userId}/balance")
    public ResponseEntity<List<LeaveBalanceInfoDto>> getUserLeaveBalance(@PathVariable Long userId) {
        return ResponseEntity.ok(leaveService.getUserLeaveBalance(userId));
    }

    @PostMapping("/users/{userId}/apply")
    public ResponseEntity<LeaveApplicationResponseDto> applyForLeave(
            @PathVariable Long userId,
            @RequestBody @Valid LeaveDto request) {
        LeaveApplicationResponseDto response = leaveService.applyForLeave(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Upload an attachment for a specific leave request using the existing EmployeeAttachment model.
     * The attachment is stored against the employee's number, with `screen` tagged as the leave request ID.
     */
    @PostMapping(
            value = "/leave-requests/{leaveRequestId}/attachments",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EmployeeAttachmentDTO> uploadLeaveRequestAttachment(
            @PathVariable Integer leaveRequestId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            Authentication authentication) throws java.io.IOException {

        // Ensure leave request exists
        LeaveRequest leaveRequest = leaveService.getLeaveRequestById(leaveRequestId);

        // Resolve current user and ensure they own the leave request
        var currentUser = userService.getCurrentUser(authentication);
        if (leaveRequest.getUser() == null || !leaveRequest.getUser().getId().equals(currentUser.getId())) {
            throw new ValidationException("Vous ne pouvez ajouter des pièces jointes qu'à vos propres demandes de congé.");
        }

        // Tag attachment with a screen identifier that references the leave request
        String screen = "LEAVE_REQUEST_" + leaveRequestId;

        EmployeeAttachmentDTO dto = attachmentService.upload(
                currentUser.getId(),
                file,
                description != null ? description : "Pièce jointe pour la demande de congé #" + leaveRequestId,
                screen,
                authentication
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/leave-requests/{leaveRequestId}/attachments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EmployeeAttachmentDTO>> listLeaveRequestAttachments(
            @PathVariable Integer leaveRequestId,
            Authentication authentication) {
        leaveService.getLeaveRequestById(leaveRequestId);
        return ResponseEntity.ok(attachmentService.listLeaveRequestAttachments(leaveRequestId));
    }

    @GetMapping("/leave-requests/{leaveRequestId}/attachments/{attachId}/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> downloadLeaveRequestAttachment(
            @PathVariable Integer leaveRequestId,
            @PathVariable Integer attachId,
            Authentication authentication) {
        var att = attachmentService.downloadLeaveRequestAttachment(leaveRequestId, attachId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(att.getFileType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + att.getFilename() + "\"")
                .body(att.getAttachment());
    }

    @DeleteMapping("/leave-requests/{leaveRequestId}/attachments/{attachId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteLeaveRequestAttachment(
            @PathVariable Integer leaveRequestId,
            @PathVariable Integer attachId,
            Authentication authentication) {
        LeaveRequest leaveRequest = leaveService.getLeaveRequestById(leaveRequestId);
        var currentUser = userService.getCurrentUser(authentication);
        if (leaveRequest.getUser() == null || !leaveRequest.getUser().getId().equals(currentUser.getId())) {
            throw new ValidationException("Vous ne pouvez supprimer que les pièces jointes de vos propres demandes de congé.");
        }
        attachmentService.deleteLeaveRequestAttachment(currentUser.getId(), leaveRequestId, attachId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Re-tag an existing attachment onto a different leave request. Used when editing a leave
     * request (implemented as delete-old + create-new) to carry over attachments the user kept.
     */
    @PutMapping("/leave-requests/{leaveRequestId}/attachments/{attachId}/reassign")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EmployeeAttachmentDTO> reassignLeaveRequestAttachment(
            @PathVariable Integer leaveRequestId,
            @PathVariable Integer attachId,
            Authentication authentication) {
        LeaveRequest leaveRequest = leaveService.getLeaveRequestById(leaveRequestId);
        var currentUser = userService.getCurrentUser(authentication);
        if (leaveRequest.getUser() == null || !leaveRequest.getUser().getId().equals(currentUser.getId())) {
            throw new ValidationException("Vous ne pouvez ajouter des pièces jointes qu'à vos propres demandes de congé.");
        }
        EmployeeAttachmentDTO dto = attachmentService.reassignToLeaveRequest(currentUser.getId(), attachId, leaveRequestId);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/leave-requests/{id}")
    public ResponseEntity<Void> deleteLeaveRequest(
            @PathVariable Integer id,
            @RequestParam Long userId) {
        leaveService.deleteLeaveRequest(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/leave-requests/{id}/cancel")
    public ResponseEntity<Void> cancelLeaveRequest(
            @PathVariable Integer id,
            @RequestParam Long userId) {
        leaveService.cancelLeaveRequest(id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pending-requests")
    @PreAuthorize("hasRole('HR_MANAGER') or hasRole('MANAGER')")
    public ResponseEntity<List<LeaveRequestSummaryDto>> getAllPendingRequests(Authentication authentication) {
        var currentUser = userService.getCurrentUser(authentication);
        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        return ResponseEntity.ok(leaveService.getAllPendingRequests(currentUser.getId(), roles));
    }

    @GetMapping("/approved-requests")
    @PreAuthorize("hasRole('HR_MANAGER') or hasRole('MANAGER')")
    public ResponseEntity<List<LeaveRequestSummaryDto>> getApprovedRequests(Authentication authentication) {
        var currentUser = userService.getCurrentUser(authentication);
        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        return ResponseEntity.ok(leaveService.getApprovedRequests(currentUser.getId(), roles));
    }

    @GetMapping("/rejected-requests")
    @PreAuthorize("hasRole('HR_MANAGER') or hasRole('MANAGER')")
    public ResponseEntity<List<LeaveRequestSummaryDto>> getRejectedRequests(Authentication authentication) {
        var currentUser = userService.getCurrentUser(authentication);
        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        return ResponseEntity.ok(leaveService.getRejectedRequests(currentUser.getId(), roles));
    }

    @PatchMapping("/leave-requests/{id}/cancel-approved")
    @PreAuthorize("hasRole('HR_MANAGER') or hasRole('MANAGER')")
    public ResponseEntity<Void> cancelApprovedLeaveRequest(
            @PathVariable Integer id,
            @RequestBody(required = false) LeaveDecisionRequest request,
            Authentication authentication) {
        var currentUser = userService.getCurrentUser(authentication);
        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        LeaveDecisionRequest decision = request != null ? request : new LeaveDecisionRequest();
        if (decision.getDecidedByUserId() == null) {
            decision.setDecidedByUserId(currentUser.getId());
        }
        leaveService.cancelApprovedLeaveRequest(id, decision, currentUser.getId(), roles);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/leave-requests/{id}/cancel-rejection")
    @PreAuthorize("hasRole('HR_MANAGER') or hasRole('MANAGER')")
    public ResponseEntity<Void> cancelRejectedLeaveRequest(
            @PathVariable Integer id,
            @RequestBody(required = false) LeaveDecisionRequest request,
            Authentication authentication) {
        var currentUser = userService.getCurrentUser(authentication);
        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        LeaveDecisionRequest decision = request != null ? request : new LeaveDecisionRequest();
        if (decision.getDecidedByUserId() == null) {
            decision.setDecidedByUserId(currentUser.getId());
        }
        leaveService.cancelRejectedLeaveRequest(id, decision, currentUser.getId(), roles);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/leave-requests/{id}/approve")
    @PreAuthorize("hasRole('HR_MANAGER') or hasRole('MANAGER')")
    public ResponseEntity<LeaveRequest> approveLeaveRequest(
            @PathVariable Integer id,
            @RequestBody(required = false) LeaveDecisionRequest request,
            Authentication authentication) {
        var currentUser = userService.getCurrentUser(authentication);
        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        LeaveDecisionRequest decision = request != null ? request : new LeaveDecisionRequest();
        if (decision.getDecidedByUserId() == null) {
            decision.setDecidedByUserId(currentUser.getId());
        }
        LeaveRequest result = leaveService.approveLeaveRequest(
                id, decision, currentUser.getId(), roles);
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/leave-requests/{id}/reject")
    @PreAuthorize("hasRole('HR_MANAGER') or hasRole('MANAGER')")
    public ResponseEntity<LeaveRequest> rejectLeaveRequest(
            @PathVariable Integer id,
            @RequestBody(required = false) LeaveDecisionRequest request,
            Authentication authentication) {
        var currentUser = userService.getCurrentUser(authentication);
        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        LeaveDecisionRequest decision = request != null ? request : new LeaveDecisionRequest();
        if (decision.getDecidedByUserId() == null) {
            decision.setDecidedByUserId(currentUser.getId());
        }
        LeaveRequest result = leaveService.rejectLeaveRequest(
                id, decision, currentUser.getId(), roles);
        return ResponseEntity.ok(result);
    }
}

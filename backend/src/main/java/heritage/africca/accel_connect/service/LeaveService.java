package heritage.africca.accel_connect.service;

import heritage.africca.accel_connect.dto.*;
import heritage.africca.accel_connect.config.LeaveObserverProperties;
import heritage.africca.accel_connect.entity.Leave;
import heritage.africca.accel_connect.entity.enumeration.LeaveStatus;
import heritage.africca.accel_connect.entity.enumeration.LeaveTypeCode;
import heritage.africca.accel_connect.entity.LeaveRequest;
import heritage.africca.accel_connect.entity.LeaveRequestComment;
import heritage.africca.accel_connect.entity.User;
import heritage.africca.accel_connect.exception.ResourceNotFoundException;
import heritage.africca.accel_connect.exception.ValidationException;
import org.springframework.security.access.AccessDeniedException;
import heritage.africca.accel_connect.repository.LeaveBalanceRepository;
import heritage.africca.accel_connect.repository.LeaveRequestRepository;
import heritage.africca.accel_connect.repository.TeamRepository;
import heritage.africca.accel_connect.repository.UserRepository;
import heritage.africca.accel_connect.service.Impl.NextcloudTalkService;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.LinkedHashSet;

import static java.util.Collections.emptyList;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaveService {

    private final UserRepository userRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveBalanceService leaveBalanceService;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final NextcloudTalkService nextcloudTalkService;
    private final LeaveObserverProperties observerProperties;
    private final SenegalHolidayService senegalHolidayService;
    private final TeamRepository teamRepository;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    private static final int DURATION_FULL_DAY = 0;
    private static final BigDecimal STANDARD_WORK_DAY_HOURS = new BigDecimal("8.00");


    @Transactional
    public LeaveApplicationResponseDto applyForLeave(Long userId, LeaveDto application) {
        log.info("Processing leave application for user ID: {}", userId);

        // Validate user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ValidationException("User not found"));

        // Resolve leave type from enum
        LeaveTypeCode leaveType = LeaveTypeCode.fromCode(application.getLeaveTypeCode())
                .orElseThrow(() -> new ValidationException("Unknown leave type: " + application.getLeaveTypeCode()));

        // Validate dates
        validateDates(application, leaveType);

        // Get work week configuration (standard Mon-Fri)
        WorkWeek workWeek = getDefaultWorkWeek();

        // Get Senegal public holidays for the requested date range
        Set<LocalDate> holidayDates = getHolidayDates(application.getStartDate(), application.getEndDate());

        // Calculate leave days for each date in range
        List<LeaveDayCalculation> leaveDays = calculateLeaveDays(application, holidayDates, workWeek);

        // Step 7: Calculate total working days in the requested range
        BigDecimal totalDeductibleDays = calculateTotalDeductibleDays(leaveDays);

        if (totalDeductibleDays.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("No valid working days in the selected date range");
        }

        // Apply leave type rules — MALADIE = 0, event leave = max(0, days - allocated)
        BigDecimal balanceDeductibleDays = resolveDeductibleDays(leaveType, totalDeductibleDays);

        // Get current leave balance
        LeaveBalanceInfoDto balance = getCurrentLeaveBalance(userId, leaveType);

        // Create leave request
        LeaveRequest leaveRequest = createLeaveRequest(user, leaveType, application, leaveDays);

        // Save and return response
        LeaveRequest savedRequest = leaveRequestRepository.save(leaveRequest);

        // Notify designated approvers by email (async — does not block)
        emailService.notifyApproversOfNewLeaveRequest(savedRequest);

        String employeeName = user.getFirstName() + " " + user.getLastName();
        for (User approver : leaveRequest.getApprovers()) {
            notificationService.createForUser(
                    approver.getId(),
                    "Nouvelle demande de congé",
                    employeeName + " a soumis une demande de congé (#" + savedRequest.getId() + ")",
                    frontendUrl + "/rh/approbation-conges"
            );
        }

        // Notify optional observers (CC users)
        List<User> observers = resolveObservers(application.getNotifyUserIds());
        emailService.notifyObserversOfNewLeaveRequest(savedRequest, observers);
        for (User observer : observers) {
            notificationService.createForUser(
                    observer.getId(),
                    "Information — demande de congé",
                    employeeName + " a soumis une demande de congé (#" + savedRequest.getId() + ")",
                    frontendUrl + "/rh/conges"
            );
        }

        notifyObservers(savedRequest, employeeName);

        // Nextcloud Talk — approvers & notifiers (fire-and-forget)
        DateTimeFormatter talkFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String talkStart = savedRequest.getLeaves().stream().map(Leave::getDate)
                .filter(Objects::nonNull).min(LocalDate::compareTo)
                .map(d -> d.format(talkFmt)).orElse("—");
        String talkEnd = savedRequest.getLeaves().stream().map(Leave::getDate)
                .filter(Objects::nonNull).max(LocalDate::compareTo)
                .map(d -> d.format(talkFmt)).orElse("—");
        String talkLeaveType = savedRequest.getLeaveType() != null ? savedRequest.getLeaveType().getCode() : "Congé";
        String approvalLink = frontendUrl + "/rh/approbation-conges";

        for (User approver : savedRequest.getApprovers()) {
            if (approver.getKeycloakId() == null) continue;
            String talkId = "SSO-" + approver.getKeycloakId();
            String msg = "📋 Nouvelle demande de congé de " + employeeName
                    + " (" + talkLeaveType + ") du " + talkStart + " au " + talkEnd
                    + " (#" + savedRequest.getId() + "). Traiter : " + approvalLink;
            CompletableFuture.runAsync(() -> nextcloudTalkService.sendDirectMessage(talkId, msg));
        }

        for (User observer : observers) {
            if (observer.getKeycloakId() == null) continue;
            String talkId = "SSO-" + observer.getKeycloakId();
            String msg = "ℹ️ Pour information : " + employeeName
                    + " a soumis une demande de congé (" + talkLeaveType + ") du " + talkStart + " au " + talkEnd + ".";
            CompletableFuture.runAsync(() -> nextcloudTalkService.sendDirectMessage(talkId, msg));
        }

        return buildResponse(savedRequest, balance, totalDeductibleDays, balanceDeductibleDays);
    }

    /**
     * Get a leave request by id with associated leaves and user.
     */
    @Transactional(readOnly = true)
    public LeaveRequest getLeaveRequestById(Integer id) {
        return leaveRequestRepository.findByIdWithAssociations(id)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", id));
    }

    /**
     * Approve a leave request.
     */
    @Transactional
    public LeaveRequest approveLeaveRequest(Integer leaveRequestId, LeaveDecisionRequest request,
                                            Long currentUserId, Set<String> currentUserRoles) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new ValidationException("Leave request not found"));

        validateApproverCanAct(leaveRequest, currentUserId, currentUserRoles);
        validateLeaveRequestCanBeModified(leaveRequest);

        BigDecimal totalDaysToDeduct = BigDecimal.ZERO;
        for (Leave leave : leaveRequest.getLeaves()) {
            if (leave.getStatus() == LeaveStatus.PENDING_APPROVAL) {
                leave.setStatus(LeaveStatus.SCHEDULED);
                if (shouldDeductFromBalance(leave)) {
                    BigDecimal days = leave.getLengthDays() != null ? leave.getLengthDays() : BigDecimal.ZERO;
                    totalDaysToDeduct = totalDaysToDeduct.add(days);
                }
            }
        }

        // Apply leave type rules before touching the balance
        BigDecimal actualDaysToDeduct = resolveDeductibleDays(leaveRequest.getLeaveType(), totalDaysToDeduct);

        if (actualDaysToDeduct.compareTo(BigDecimal.ZERO) > 0) {
            Long userId = leaveRequest.getUser() != null ? leaveRequest.getUser().getId() : null;
            if (userId != null) {
                leaveBalanceService.deductBalanceForUser(userId, actualDaysToDeduct);
            }
        }

        leaveRequest.setDecidedBy(userRepository.findById(currentUserId).orElse(null));
        addDecisionComment(leaveRequest, request, "Approved");
        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);
        notificationService.createForUser(
                leaveRequest.getUser().getId(),
                "Congé approuvé",
                "Votre demande de congé #" + leaveRequestId + " a été approuvée.",
                frontendUrl + "/rh/conges"
        );
        emailService.notifyEmployeeOfLeaveApproval(saved);
        return saved;
    }

    private void notifyObservers(LeaveRequest saved, String employeeName) {
        if (observerProperties.getEmails().isEmpty()) return;
        Set<String> approverEmails = saved.getApprovers().stream()
                .filter(a -> a != null && a.getEmail() != null)
                .map(User::getEmail)
                .collect(Collectors.toSet());
        List<User> observers = observerProperties.getEmails().stream()
                .filter(email -> !approverEmails.contains(email))
                .map(email -> userRepository.findFirstByEmail(email).orElse(null))
                .filter(u -> u != null)
                .toList();
        observers.forEach(observer -> notificationService.createForUser(
                observer.getId(),
                "Nouvelle demande de congé",
                employeeName + " a soumis une demande de congé (#" + saved.getId() + ").",
                frontendUrl + "/rh/conges"
        ));
        emailService.notifyObserversOfNewLeaveRequest(saved, observers);
    }

    /**
     * Reject a leave request.
     */
    @Transactional
    public LeaveRequest rejectLeaveRequest(Integer leaveRequestId, LeaveDecisionRequest request,
                                           Long currentUserId, Set<String> currentUserRoles) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new ValidationException("Leave request not found"));

        validateApproverCanAct(leaveRequest, currentUserId, currentUserRoles);
        validateLeaveRequestCanBeModified(leaveRequest);

        for (Leave leave : leaveRequest.getLeaves()) {
            if (leave.getStatus() == LeaveStatus.PENDING_APPROVAL) {
                leave.setStatus(LeaveStatus.REJECTED);
            }
        }

        leaveRequest.setDecidedBy(userRepository.findById(currentUserId).orElse(null));
        addDecisionComment(leaveRequest, request, "Rejected");
        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);
        notificationService.createForUser(
                leaveRequest.getUser().getId(),
                "Congé rejeté",
                "Votre demande de congé #" + leaveRequestId + " a été rejetée.",
                frontendUrl + "/rh/conges"
        );
        emailService.notifyEmployeeOfLeaveRejection(saved);
        return saved;
    }

    /**
     * Get all leave requests for a user, most recent first
     */
    @Transactional(readOnly = true)
    public List<LeaveRequestSummaryDto> getUserLeaveRequests(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", userId);
        }
        return leaveRequestRepository.findByUserIdWithDetails(userId).stream()
                .sorted(Comparator.comparing(LeaveRequest::getDateApplied).reversed())
                .map(this::toLeaveRequestSummary)
                .collect(Collectors.toList());
    }

    /**
     * Get leave balance for all leave types for a given user
     */
    @Transactional(readOnly = true)
    public List<LeaveBalanceInfoDto> getUserLeaveBalance(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", userId);
        }
        return Arrays.stream(LeaveTypeCode.values())
                .map(ltc -> getCurrentLeaveBalance(userId, ltc))
                .collect(Collectors.toList());
    }

    /**
     * Get all pending requests (managers see only their designated requests).
     */
    @Transactional(readOnly = true)
    public List<LeaveRequestSummaryDto> getAllPendingRequests(Long currentUserId, Set<String> currentUserRoles) {

        List<LeaveRequest> requests = leaveRequestRepository.findPendingLeavesByApprover(currentUserId);

        return requests.stream()
                .sorted(Comparator.comparing(LeaveRequest::getDateApplied).reversed())
                .map(lr -> {
                    LeaveRequestSummaryDto dto = toLeaveRequestSummary(lr);
                    if (lr.getUser() != null) {
                        String name = ((lr.getUser().getFirstName() != null ? lr.getUser().getFirstName() : "") +
                                " " + (lr.getUser().getLastName() != null ? lr.getUser().getLastName() : "")).trim();
                        dto.setEmployeeName(name.isEmpty() ? lr.getUser().getEmail() : name);
                        dto.setEmployeeEmail(lr.getUser().getEmail());
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Legacy method for backward compatibility. Use getAllPendingRequests(Long, Set) instead.
     */
    @Transactional(readOnly = true)
    @Deprecated
    public List<LeaveRequestSummaryDto> getAllPendingRequests() {
        return leaveRequestRepository.findAllWithPendingLeaves().stream()
                .sorted(Comparator.comparing(LeaveRequest::getDateApplied).reversed())
                .map(lr -> {
                    LeaveRequestSummaryDto dto = toLeaveRequestSummary(lr);
                    if (lr.getUser() != null) {
                        String name = ((lr.getUser().getFirstName() != null ? lr.getUser().getFirstName() : "") +
                                " " + (lr.getUser().getLastName() != null ? lr.getUser().getLastName() : "")).trim();
                        dto.setEmployeeName(name.isEmpty() ? lr.getUser().getEmail() : name);
                        dto.setEmployeeEmail(lr.getUser().getEmail());
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestSummaryDto> getApprovedRequests(Long currentUserId, Set<String> currentUserRoles) {
        boolean isHrManager = currentUserRoles != null && currentUserRoles.contains("ROLE_HR_MANAGER");

        List<LeaveRequest> requests;
        if (isHrManager) {
            requests = leaveRequestRepository.findAllWithApprovedLeaves();
        } else {
            List<Long> supervisedTeamIds = getSupervisedTeamIds(currentUserId);
            if (!supervisedTeamIds.isEmpty()) {
                Set<LeaveRequest> combined = new LinkedHashSet<>(leaveRequestRepository.findApprovedLeavesByApprover(currentUserId));
                combined.addAll(leaveRequestRepository.findApprovedByTeams(supervisedTeamIds));
                requests = new ArrayList<>(combined);
            } else {
                requests = leaveRequestRepository.findApprovedLeavesByApprover(currentUserId);
            }
        }

        return requests.stream()
                .sorted(Comparator.comparing(LeaveRequest::getDateApplied).reversed())
                .map(lr -> {
                    LeaveRequestSummaryDto dto = toLeaveRequestSummary(lr);
                    if (lr.getUser() != null) {
                        String name = ((lr.getUser().getFirstName() != null ? lr.getUser().getFirstName() : "") +
                                " " + (lr.getUser().getLastName() != null ? lr.getUser().getLastName() : "")).trim();
                        dto.setEmployeeName(name.isEmpty() ? lr.getUser().getEmail() : name);
                        dto.setEmployeeEmail(lr.getUser().getEmail());
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private List<Long> getSupervisedTeamIds(Long userId) {
        return teamRepository.findSupervisedBy(userId)
                .stream().map(heritage.africca.accel_connect.entity.Team::getId).toList();
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestSummaryDto> getRejectedRequests(Long currentUserId, Set<String> currentUserRoles) {
        boolean isHrManager = currentUserRoles != null && currentUserRoles.contains("ROLE_HR_MANAGER");

        List<LeaveRequest> requests;
        if (isHrManager) {
            requests = leaveRequestRepository.findAllWithRejectedLeaves();
        } else {
            List<Long> supervisedTeamIds = getSupervisedTeamIds(currentUserId);
            if (!supervisedTeamIds.isEmpty()) {
                Set<LeaveRequest> combined = new LinkedHashSet<>(leaveRequestRepository.findRejectedLeavesByApprover(currentUserId));
                combined.addAll(leaveRequestRepository.findRejectedByTeams(supervisedTeamIds));
                requests = new ArrayList<>(combined);
            } else {
                requests = leaveRequestRepository.findRejectedLeavesByApprover(currentUserId);
            }
        }

        return requests.stream()
                .sorted(Comparator.comparing(LeaveRequest::getDateApplied).reversed())
                .map(lr -> {
                    LeaveRequestSummaryDto dto = toLeaveRequestSummary(lr);
                    if (lr.getUser() != null) {
                        String name = ((lr.getUser().getFirstName() != null ? lr.getUser().getFirstName() : "") +
                                " " + (lr.getUser().getLastName() != null ? lr.getUser().getLastName() : "")).trim();
                        dto.setEmployeeName(name.isEmpty() ? lr.getUser().getEmail() : name);
                        dto.setEmployeeEmail(lr.getUser().getEmail());
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public void validateLeaveRequestViewAccess(LeaveRequest leaveRequest, Long currentUserId, Set<String> currentUserRoles) {
        if (leaveRequest == null) {
            throw new ResourceNotFoundException("LeaveRequest not found");
        }
        if (leaveRequest.getUser() != null && leaveRequest.getUser().getId().equals(currentUserId)) {
            return;
        }
        if (currentUserRoles != null && currentUserRoles.contains("ROLE_HR_MANAGER")) {
            return;
        }
        List<User> approvers = leaveRequest.getApprovers();
        if (approvers != null && approvers.stream()
                .anyMatch(a -> a != null && a.getId() != null && a.getId().equals(currentUserId))) {
            return;
        }
        throw new AccessDeniedException("You do not have permission to view this leave request");
    }

    /**
     * Hard-delete a pending leave request — only the owner can delete.
     */
    @Transactional
    public void deleteLeaveRequest(Integer leaveRequestId, Long userId) {
        LeaveRequest lr = leaveRequestRepository.findByIdWithAssociations(leaveRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", leaveRequestId));

        if (!lr.getUser().getId().equals(userId)) {
            throw new ValidationException("Vous ne pouvez supprimer que vos propres demandes de congé.");
        }

        boolean hasPending = lr.getLeaves() != null && lr.getLeaves().stream()
                .anyMatch(l -> l.getStatus() == LeaveStatus.PENDING_APPROVAL);
        if (!hasPending) {
            throw new ValidationException("Seules les demandes en attente d'approbation peuvent être supprimées.");
        }

        leaveRequestRepository.delete(lr);
        log.info("LeaveRequest {} deleted by user {}", leaveRequestId, userId);
    }

    /**
     * Cancel an already-approved (SCHEDULED) leave request — only HR_MANAGER or a designated MANAGER can do this.
     * Restores the deducted balance.
     */
    @Transactional
    public void cancelApprovedLeaveRequest(Integer leaveRequestId, LeaveDecisionRequest request,
                                           Long currentUserId, Set<String> currentUserRoles) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", leaveRequestId));

        validateApproverCanAct(leaveRequest, currentUserId, currentUserRoles);

        boolean hasScheduled = leaveRequest.getLeaves() != null && leaveRequest.getLeaves().stream()
                .anyMatch(l -> l.getStatus() == LeaveStatus.SCHEDULED);
        if (!hasScheduled) {
            throw new ValidationException("Seuls les congés approuvés (planifiés) peuvent être annulés.");
        }

        BigDecimal totalDaysToRestore = BigDecimal.ZERO;
        for (Leave leave : leaveRequest.getLeaves()) {
            if (leave.getStatus() == LeaveStatus.SCHEDULED) {
                leave.setStatus(LeaveStatus.CANCELLED);
                if (shouldDeductFromBalance(leave)) {
                    BigDecimal days = leave.getLengthDays() != null ? leave.getLengthDays() : BigDecimal.ZERO;
                    totalDaysToRestore = totalDaysToRestore.add(days);
                }
            }
        }

        BigDecimal actualDaysToRestore = resolveDeductibleDays(leaveRequest.getLeaveType(), totalDaysToRestore);
        if (actualDaysToRestore.compareTo(BigDecimal.ZERO) > 0) {
            Long userId = leaveRequest.getUser() != null ? leaveRequest.getUser().getId() : null;
            if (userId != null) {
                leaveBalanceService.addBalanceForUser(userId, actualDaysToRestore);
            }
        }

        addDecisionComment(leaveRequest, request, "Cancelled");
        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);

        if (leaveRequest.getUser() != null) {
            notificationService.createForUser(
                    leaveRequest.getUser().getId(),
                    "Congé annulé",
                    "Votre demande de congé #" + leaveRequestId + " a été annulée.",
                    frontendUrl + "/rh/conges"
            );
            emailService.notifyEmployeeOfLeaveCancellation(saved);
        }
    }

    /**
     * Cancel a rejection — only HR_MANAGER or a designated MANAGER can do this.
     * The request goes back to pending approval. No balance impact: rejection never deducts days.
     */
    @Transactional
    public void cancelRejectedLeaveRequest(Integer leaveRequestId, LeaveDecisionRequest request,
                                           Long currentUserId, Set<String> currentUserRoles) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", leaveRequestId));

        validateApproverCanAct(leaveRequest, currentUserId, currentUserRoles);

        boolean hasRejected = leaveRequest.getLeaves() != null && leaveRequest.getLeaves().stream()
                .anyMatch(l -> l.getStatus() == LeaveStatus.REJECTED);
        if (!hasRejected) {
            throw new ValidationException("Seules les demandes rejetées peuvent être remises en attente.");
        }

        for (Leave leave : leaveRequest.getLeaves()) {
            if (leave.getStatus() == LeaveStatus.REJECTED) {
                leave.setStatus(LeaveStatus.PENDING_APPROVAL);
            }
        }

        leaveRequest.setDecidedBy(null);
        addDecisionComment(leaveRequest, request, "Rejection cancelled");
        leaveRequestRepository.save(leaveRequest);

        if (leaveRequest.getUser() != null) {
            notificationService.createForUser(
                    leaveRequest.getUser().getId(),
                    "Rejet annulé",
                    "Votre demande de congé #" + leaveRequestId + " est de nouveau en attente d'approbation.",
                    frontendUrl + "/rh/conges"
            );
        }
    }

    @Transactional
    public void cancelLeaveRequest(Integer leaveRequestId, Long userId) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", leaveRequestId));

        if (!leaveRequest.getUser().getId().equals(userId)) {
            throw new ValidationException("You can only cancel your own leave requests");
        }

        boolean hasPending = leaveRequest.getLeaves().stream()
                .anyMatch(l -> l.getStatus() == LeaveStatus.PENDING_APPROVAL);
        if (!hasPending) {
            throw new ValidationException("Only pending leave requests can be cancelled");
        }

        for (Leave leave : leaveRequest.getLeaves()) {
            if (leave.getStatus() == LeaveStatus.PENDING_APPROVAL) {
                leave.setStatus(LeaveStatus.CANCELLED);
            }
        }
        leaveRequestRepository.save(leaveRequest);
    }


    private BigDecimal resolveDeductibleDays(LeaveTypeCode leaveType, BigDecimal requestedDays) {
        if (leaveType == null) return requestedDays;
        return switch (leaveType.getDeductionRule()) {
            case NONE -> BigDecimal.ZERO;
            case EXCESS_ONLY -> {
                BigDecimal allocated = BigDecimal.valueOf(leaveType.getAllocatedDays());
                BigDecimal excess = requestedDays.subtract(allocated);
                yield excess.compareTo(BigDecimal.ZERO) > 0 ? excess : BigDecimal.ZERO;
            }
            case FULL -> requestedDays;
        };
    }


    private LeaveRequestSummaryDto toLeaveRequestSummary(LeaveRequest lr) {
        List<Leave> leaves = lr.getLeaves() != null ? lr.getLeaves() : List.of();

        LocalDate startDate = leaves.stream().map(Leave::getDate).filter(Objects::nonNull)
                .min(LocalDate::compareTo).orElse(lr.getDateApplied());
        LocalDate endDate = leaves.stream().map(Leave::getDate).filter(Objects::nonNull)
                .max(LocalDate::compareTo).orElse(lr.getDateApplied());

        BigDecimal totalDays = leaves.stream()
                .map(l -> l.getLengthDays() != null ? l.getLengthDays() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<LeaveDayDetailDto> days = leaves.stream().map(this::toLeaveDayDetail).collect(Collectors.toList());

        List<LeaveRequestSummaryDto.CommentDto> comments = lr.getComments() != null
                ? lr.getComments().stream()
                        .map(c -> LeaveRequestSummaryDto.CommentDto.builder()
                                .text(c.getComments())
                                .author(c.getCreatedBy() != null
                                        ? c.getCreatedBy().getFirstName() + " " + c.getCreatedBy().getLastName()
                                        : "Système")
                                .createdAt(c.getCreated())
                                .build())
                        .collect(Collectors.toList())
                : List.of();

        List<LeaveRequestSummaryDto.ApproverDto> approvers = (lr.getApprovers() != null ? lr.getApprovers() : List.<User>of())
                .stream().filter(Objects::nonNull)
                .map(a -> LeaveRequestSummaryDto.ApproverDto.builder()
                        .id(a.getId())
                        .displayName(((a.getFirstName() != null ? a.getFirstName() : "") + " "
                                + (a.getLastName() != null ? a.getLastName() : "")).trim())
                        .build())
                .collect(Collectors.toList());

        String decidedByName = lr.getDecidedBy() != null
                ? lr.getDecidedBy().getFirstName() + " " + lr.getDecidedBy().getLastName()
                : null;

        return LeaveRequestSummaryDto.builder()
                .id(lr.getId())
                .leaveTypeCode(lr.getLeaveType() != null ? lr.getLeaveType().getCode() : null)
                .leaveTypeName(lr.getLeaveType() != null ? lr.getLeaveType().getDisplayName() : "")
                .dateApplied(lr.getDateApplied())
                .startDate(startDate).endDate(endDate).totalDays(totalDays)
                .overallStatus(deriveOverallStatus(leaves))
                .days(days).comments(comments)
                .approvers(approvers.isEmpty() ? null : approvers)
                .decidedByName(decidedByName)
                .build();
    }

    private String deriveOverallStatus(List<Leave> leaves) {
        if (leaves == null || leaves.isEmpty()) return "Inconnu";
        Set<LeaveStatus> statuses = leaves.stream().map(Leave::getStatus)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (statuses.contains(LeaveStatus.PENDING_APPROVAL)) return "En attente";
        if (statuses.contains(LeaveStatus.SCHEDULED))        return "Approuvé";
        if (statuses.contains(LeaveStatus.TAKEN))            return "Pris";
        if (statuses.contains(LeaveStatus.REJECTED))         return "Rejeté";
        if (statuses.contains(LeaveStatus.CANCELLED))        return "Annulé";
        return "Inconnu";
    }

    private LeaveDayDetailDto toLeaveDayDetail(Leave leave) {
        return LeaveDayDetailDto.builder()
                .date(leave.getDate())
                .dayOfWeek(leave.getDate() != null
                        ? leave.getDate().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.getDefault()) : "")
                .lengthDays(leave.getLengthDays()).lengthHours(leave.getLengthHours())
                .status(leave.getStatus() != null ? leave.getStatus().getDisplayName() : "Unknown")
                .durationType(getDurationDisplay(leave.getDurationType()))
                .isWeekend(false).isHoliday(false).build();
    }

    private List<User> resolveApprovers(List<Long> approverUserIds) {
        if (approverUserIds == null || approverUserIds.isEmpty()) return new ArrayList<>();
        List<User> approvers = new ArrayList<>();
        for (Long id : approverUserIds) {
            userRepository.findById(id).ifPresent(approvers::add);
        }
        return approvers;
    }

    private List<User> resolveObservers(List<Long> notifyUserIds) {
        if (notifyUserIds == null || notifyUserIds.isEmpty()) return new ArrayList<>();
        List<User> observers = new ArrayList<>();
        for (Long id : notifyUserIds) {
            userRepository.findById(id).ifPresent(observers::add);
        }
        return observers;
    }

    private void validateApproverCanAct(LeaveRequest leaveRequest, Long currentUserId, Set<String> currentUserRoles) {
        List<User> approvers = leaveRequest.getApprovers() != null ? leaveRequest.getApprovers() : emptyList();
        if (currentUserRoles != null && currentUserRoles.contains("ROLE_HR_MANAGER")) return;
        if (!approvers.isEmpty()) {
            boolean isDesignated = approvers.stream()
                    .anyMatch(a -> a != null && a.getId() != null && a.getId().equals(currentUserId));
            if (!isDesignated) {
                throw new ValidationException("Only designated approvers (or HR Manager) can approve or reject this leave request");
            }
        }
    }

    private void validateLeaveRequestCanBeModified(LeaveRequest leaveRequest) {
        boolean hasPending = leaveRequest.getLeaves() != null && leaveRequest.getLeaves().stream()
                .anyMatch(l -> LeaveStatus.PENDING_APPROVAL == l.getStatus());
        if (!hasPending) {
            throw new ValidationException("Leave request has no pending leaves to approve or reject");
        }
    }

    private void addDecisionComment(LeaveRequest leaveRequest, LeaveDecisionRequest request, String action) {
        if (request != null && request.getComment() != null && !request.getComment().isEmpty()) {
            User decidedBy = request.getDecidedByUserId() != null
                    ? userRepository.findById(request.getDecidedByUserId()).orElse(null) : null;
            LeaveRequestComment comment = LeaveRequestComment.builder()
                    .leaveRequest(leaveRequest).comments(action + ": " + request.getComment())
                    .createdBy(decidedBy).build();
            if (leaveRequest.getComments() == null) leaveRequest.setComments(new ArrayList<>());
            leaveRequest.getComments().add(comment);
        }
    }

    private List<LeaveDayCalculation> calculateLeaveDays(LeaveDto application,
                                                         Set<LocalDate> holidayDates,
                                                         WorkWeek workWeek) {
        List<LeaveDayCalculation> leaveDays = new ArrayList<>();
        LocalDate current = application.getStartDate();
        while (!current.isAfter(application.getEndDate())) {
            leaveDays.add(calculateSingleDay(current, application, holidayDates, workWeek));
            current = current.plusDays(1);
        }
        return leaveDays;
    }

    private void validateDates(LeaveDto application, LeaveTypeCode leaveType) {
        if (application.getEndDate().isBefore(application.getStartDate())) {
            throw new ValidationException("End date must be after or equal to start date");
        }
    }

    private LocalDate addWorkingDays(LocalDate from, int days) {
        LocalDate date = from;
        int added = 0;
        while (added < days) {
            date = date.plusDays(1);
            if (date.getDayOfWeek().getValue() < 6) added++;
        }
        return date;
    }

    private LeaveDayCalculation calculateSingleDay(LocalDate date, LeaveDto application,
                                                   Set<LocalDate> holidayDates, WorkWeek workWeek) {
        String dayOfWeek = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.getDefault());
        boolean isWeekend = isWeekend(date, workWeek);
        boolean isHoliday = holidayDates.contains(date);

        if (isWeekend || isHoliday) {
            return LeaveDayCalculation.builder()
                    .date(date).dayOfWeek(dayOfWeek)
                    .lengthDays(BigDecimal.ZERO).lengthHours(BigDecimal.ZERO)
                    .status(LeaveStatus.SCHEDULED).durationType(DURATION_FULL_DAY)
                    .isWeekend(isWeekend).isHoliday(isHoliday).build();
        }

        boolean isSingleDay = application.getStartDate().equals(application.getEndDate());
        if (isSingleDay && hasTimeRange(application)) {
            return calculatePartialDayLeave(date, dayOfWeek, application);
        }
        return buildFullDayLeaveCalculation(date, dayOfWeek);
    }

    private boolean hasTimeRange(LeaveDto application) {
        return application.getStartTime() != null && application.getEndTime() != null
                && application.getEndTime().isAfter(application.getStartTime());
    }

    private LeaveDayCalculation calculatePartialDayLeave(LocalDate date, String dayOfWeek, LeaveDto application) {
        BigDecimal hours = calculateDurationHours(application.getStartTime(), application.getEndTime());
        return LeaveDayCalculation.builder()
                .date(date).dayOfWeek(dayOfWeek)
                .lengthDays(calculateLengthDaysFromHours(hours)).lengthHours(hours)
                .startTime(application.getStartTime()).endTime(application.getEndTime())
                .status(LeaveStatus.PENDING_APPROVAL).durationType(determineDurationType(hours))
                .isWeekend(false).isHoliday(false).build();
    }

    private BigDecimal calculateDurationHours(LocalTime startTime, LocalTime endTime) {
        long minutes = Duration.between(startTime, endTime).toMinutes();
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateLengthDaysFromHours(BigDecimal hours) {
        return hours.divide(STANDARD_WORK_DAY_HOURS, 2, RoundingMode.HALF_UP);
    }

    private int determineDurationType(BigDecimal hours) {
        return hours.compareTo(new BigDecimal("4.00")) < 0 ? Leave.DURATION_HALF_DAY : DURATION_FULL_DAY;
    }

    private boolean shouldDeductFromBalance(Leave leave) {
        if (leave == null || leave.getLengthHours() == null) return true;
        return leave.getLengthHours().compareTo(new BigDecimal("4.00")) >= 0;
    }

    private BigDecimal calculateTotalDeductibleDays(List<LeaveDayCalculation> leaveDays) {
        return leaveDays.stream()
                .filter(ld -> ld.getLengthHours() != null
                        && ld.getLengthHours().compareTo(new BigDecimal("4.00")) >= 0)
                .map(LeaveDayCalculation::getLengthDays)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private LeaveDayCalculation buildFullDayLeaveCalculation(LocalDate date, String dayOfWeek) {
        return LeaveDayCalculation.builder()
                .date(date).dayOfWeek(dayOfWeek)
                .lengthDays(BigDecimal.ONE).lengthHours(STANDARD_WORK_DAY_HOURS)
                .status(LeaveStatus.PENDING_APPROVAL).durationType(DURATION_FULL_DAY)
                .isWeekend(false).isHoliday(false).build();
    }

    private boolean isWeekend(LocalDate date, WorkWeek workWeek) {
        return switch (date.getDayOfWeek().getValue()) {
            case 1 -> workWeek.getMonday()    == WorkWeek.DAY_TYPE_NON_WORKING_DAY;
            case 2 -> workWeek.getTuesday()   == WorkWeek.DAY_TYPE_NON_WORKING_DAY;
            case 3 -> workWeek.getWednesday() == WorkWeek.DAY_TYPE_NON_WORKING_DAY;
            case 4 -> workWeek.getThursday()  == WorkWeek.DAY_TYPE_NON_WORKING_DAY;
            case 5 -> workWeek.getFriday()    == WorkWeek.DAY_TYPE_NON_WORKING_DAY;
            case 6 -> workWeek.getSaturday()  == WorkWeek.DAY_TYPE_NON_WORKING_DAY;
            case 7 -> workWeek.getSunday()    == WorkWeek.DAY_TYPE_NON_WORKING_DAY;
            default -> false;
        };
    }



    private LeaveBalanceInfoDto getCurrentLeaveBalance(Long userId, LeaveTypeCode leaveType) {
        BigDecimal entitled = leaveBalanceRepository.findByUser_Id(userId)
                .map(lb -> lb.getBalanceDays() != null ? lb.getBalanceDays() : BigDecimal.ZERO)
                .orElse(BigDecimal.ZERO);

        BigDecimal totalPending = computeTotalPending(userId);

        List<LeaveRequest> typeRequests = leaveRequestRepository.findPendingByUserAndLeaveType(
                userId, leaveType, LocalDate.now());
        BigDecimal typeScheduled = BigDecimal.ZERO;
        BigDecimal typePending = BigDecimal.ZERO;
        LocalDate today = LocalDate.now();
        for (LeaveRequest request : typeRequests) {
            if (request.getLeaves() == null) continue;
            for (Leave leave : request.getLeaves()) {
                if (leave.getDate() == null || leave.getDate().isBefore(today)) continue;
                if (shouldDeductFromBalance(leave)) {
                    BigDecimal days = leave.getLengthDays() != null ? leave.getLengthDays() : BigDecimal.ZERO;
                    if (leave.getStatus() == LeaveStatus.SCHEDULED)
                        typeScheduled = typeScheduled.add(days);
                    else if (leave.getStatus() == LeaveStatus.PENDING_APPROVAL)
                        typePending = typePending.add(days);
                }
            }
        }

        BigDecimal available = entitled.subtract(totalPending);

        return LeaveBalanceInfoDto.builder()
                .leaveTypeCode(leaveType.getCode())
                .leaveTypeName(leaveType.getDisplayName())
                .entitled(entitled).taken(BigDecimal.ZERO)
                .scheduled(typeScheduled).pending(typePending)
                .available(available)
                .displayValue(String.format("%.2f jour(s)", available))
                .build();
    }

    /**
     * Total days currently reserved by pending requests, applying leave-type rules
     * so MALADIE/event leaves don't over-reserve the annual balance.
     */
    private BigDecimal computeTotalPending(Long userId) {
        List<LeaveRequest> requests = leaveRequestRepository.findPendingByUser(userId, LocalDate.now());
        BigDecimal pending = BigDecimal.ZERO;
        LocalDate today = LocalDate.now();
        for (LeaveRequest request : requests) {
            if (request.getLeaves() == null) continue;
            BigDecimal requestDays = BigDecimal.ZERO;
            for (Leave leave : request.getLeaves()) {
                if (leave.getDate() == null || leave.getDate().isBefore(today)) continue;
                if (leave.getStatus() == LeaveStatus.PENDING_APPROVAL && shouldDeductFromBalance(leave)) {
                    requestDays = requestDays.add(leave.getLengthDays() != null ? leave.getLengthDays() : BigDecimal.ZERO);
                }
            }
            if (requestDays.compareTo(BigDecimal.ZERO) > 0 && request.getLeaveType() != null) {
                requestDays = resolveDeductibleDays(request.getLeaveType(), requestDays);
            }
            pending = pending.add(requestDays);
        }
        return pending;
    }


    private LeaveRequest createLeaveRequest(User user, LeaveTypeCode leaveType,
                                            LeaveDto application, List<LeaveDayCalculation> leaveDays) {
        LeaveRequest leaveRequest = LeaveRequest.builder()
                .user(user).leaveType(leaveType).dateApplied(LocalDate.now())
                .leaves(new ArrayList<>()).comments(new ArrayList<>())
                .approvers(resolveApprovers(application.getApproverUserIds()))
                .build();

        if (application.getComment() != null && !application.getComment().isEmpty()) {
            leaveRequest.getComments().add(LeaveRequestComment.builder()
                    .comments(application.getComment()).createdBy(user).leaveRequest(leaveRequest).build());
        }

        for (LeaveDayCalculation calc : leaveDays) {
            if (calc.getLengthDays().compareTo(BigDecimal.ZERO) > 0) {
                leaveRequest.getLeaves().add(Leave.builder()
                        .date(calc.getDate()).lengthDays(calc.getLengthDays()).lengthHours(calc.getLengthHours())
                        .startTime(calc.getStartTime()).endTime(calc.getEndTime())
                        .status(calc.getStatus()).durationType(calc.getDurationType())
                        .leaveRequest(leaveRequest).leaveType(leaveType).user(user).build());
            }
        }
        return leaveRequest;
    }

    private LeaveApplicationResponseDto buildResponse(LeaveRequest leaveRequest, LeaveBalanceInfoDto balance,
                                                      BigDecimal daysRequested, BigDecimal balanceDeductibleDays) {
        List<LeaveDayDetailDto> leaveDays = (leaveRequest.getLeaves() != null
                ? leaveRequest.getLeaves() : List.<Leave>of()).stream()
                .map(leave -> LeaveDayDetailDto.builder()
                        .date(leave.getDate())
                        .dayOfWeek(leave.getDate().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.getDefault()))
                        .lengthDays(leave.getLengthDays()).lengthHours(leave.getLengthHours())
                        .status(leave.getStatus() != null ? leave.getStatus().getDisplayName() : "Unknown")
                        .durationType(getDurationDisplay(leave.getDurationType()))
                        .isWeekend(false).isHoliday(false).build())
                .collect(Collectors.toList());

        BigDecimal balanceAfter = balance.getAvailable().subtract(balanceDeductibleDays);

        return LeaveApplicationResponseDto.builder()
                .leaveRequestId(leaveRequest.getId())
                .status("Pending Approval")
                .message("Demande de congé soumise avec succès")
                .daysRequested(daysRequested)
                .balanceDeductibleDays(balanceDeductibleDays)
                .availableBalance(balance.getAvailable())
                .balanceAfter(balanceAfter)
                .leaveDays(leaveDays)
                .warnings(new ArrayList<>())
                .build();
    }

    private String getDurationDisplay(Integer durationType) {
        if (durationType == null) return "Unknown";
        if (durationType == DURATION_FULL_DAY) return "Full Day";
        if (durationType == Leave.DURATION_HALF_DAY) return "Demi-journée";
        return "Unknown";
    }

    private WorkWeek getDefaultWorkWeek() {
        return WorkWeek.builder()
                .monday(WorkWeek.DAY_TYPE_WORKING_DAY).tuesday(WorkWeek.DAY_TYPE_WORKING_DAY)
                .wednesday(WorkWeek.DAY_TYPE_WORKING_DAY).thursday(WorkWeek.DAY_TYPE_WORKING_DAY)
                .friday(WorkWeek.DAY_TYPE_WORKING_DAY).saturday(WorkWeek.DAY_TYPE_NON_WORKING_DAY)
                .sunday(WorkWeek.DAY_TYPE_NON_WORKING_DAY).build();
    }

    private Set<LocalDate> getHolidayDates(LocalDate start, LocalDate end) {
        return senegalHolidayService.getHolidaysForRange(start, end);
    }


    @Data
    @Builder
    private static class LeaveDayCalculation {
        private LocalDate date;
        private String dayOfWeek;
        private BigDecimal lengthDays;
        private BigDecimal lengthHours;
        private LocalTime startTime;
        private LocalTime endTime;
        private LeaveStatus status;
        private int durationType;
        private boolean isWeekend;
        private boolean isHoliday;
    }

    @Data
    @Builder
    private static class WorkWeek {
        public static final int DAY_TYPE_WORKING_DAY = 1;
        public static final int DAY_TYPE_NON_WORKING_DAY = 0;
        private int monday, tuesday, wednesday, thursday, friday, saturday, sunday;
    }
}

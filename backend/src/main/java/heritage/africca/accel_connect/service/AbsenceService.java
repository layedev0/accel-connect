package heritage.africca.accel_connect.service;

import heritage.africca.accel_connect.dto.AbsenceDecisionRequest;
import heritage.africca.accel_connect.dto.AbsenceRequestDto;
import heritage.africca.accel_connect.dto.AbsenceSummaryDto;
import heritage.africca.accel_connect.config.LeaveObserverProperties;
import heritage.africca.accel_connect.entity.AbsenceComment;
import heritage.africca.accel_connect.entity.AbsenceRequest;
import heritage.africca.accel_connect.entity.User;
import heritage.africca.accel_connect.entity.enumeration.AbsenceStatus;
import heritage.africca.accel_connect.entity.enumeration.AbsenceTypeCode;
import heritage.africca.accel_connect.exception.ResourceNotFoundException;
import heritage.africca.accel_connect.exception.ValidationException;
import heritage.africca.accel_connect.repository.AbsenceRequestRepository;
import heritage.africca.accel_connect.repository.TeamRepository;
import heritage.africca.accel_connect.repository.UserRepository;
import heritage.africca.accel_connect.service.Impl.NextcloudTalkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AbsenceService {

    private static final DateTimeFormatter REF_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final AbsenceRequestRepository absenceRequestRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final NextcloudTalkService nextcloudTalkService;
    private final LeaveObserverProperties observerProperties;
    private final TeamRepository teamRepository;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Transactional
    public AbsenceSummaryDto submitAbsence(Long userId, AbsenceRequestDto dto) {
        User employee = findUser(userId);

        validateDate(dto.getDate());
        validateTimes(dto.getStartTime(), dto.getEndTime());

        BigDecimal duration = computeDuration(dto.getStartTime(), dto.getEndTime());

        Set<User> approvers = resolveApprovers(dto.getApproverUserIds());

        AbsenceRequest request = AbsenceRequest.builder()
                .user(employee)
                .absenceType(AbsenceTypeCode.fromJson(dto.getAbsenceType()))
                .date(dto.getDate())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .durationHours(duration)
                .reason(dto.getReason())
                .status(AbsenceStatus.PENDING_APPROVAL)
                .referenceCode(generateReferenceCode(dto.getDate()))
                .dateApplied(LocalDate.now())
                .approvers(approvers)
                .comments(new HashSet<>())
                .build();

        AbsenceRequest saved = absenceRequestRepository.save(request);
        emailService.notifyApproversOfNewAbsenceRequest(saved);

        String employeeName = employee.getFirstName() + " " + employee.getLastName();
        for (User approver : approvers) {
            notificationService.createForUser(
                    approver.getId(),
                    "Nouvelle demande d'absence",
                    employeeName + " a soumis une demande d'absence (" + saved.getReferenceCode() + ")",
                    frontendUrl + "/rh/approbation-absences"
            );
        }

        notifyObservers(saved, employeeName);

        // Nextcloud Talk — approver (fire-and-forget)
        DateTimeFormatter talkFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String talkDate = saved.getDate() != null ? saved.getDate().format(talkFmt) : "—";
        String talkStart = saved.getStartTime() != null ? saved.getStartTime().toString().substring(0, 5) : "—";
        String talkEnd = saved.getEndTime() != null ? saved.getEndTime().toString().substring(0, 5) : "—";
        String talkType = saved.getAbsenceType() != null ? saved.getAbsenceType().getDisplayName() : "Absence";
        String approvalLink = frontendUrl + "/rh/approbation-absences";

        for (User approver : approvers) {
            if (approver.getKeycloakId() == null) continue;
            String talkId = "SSO-" + approver.getKeycloakId();
            String msg = "📋 Nouvelle demande d'absence de " + employeeName
                    + " (" + talkType + ") le " + talkDate
                    + " de " + talkStart + " à " + talkEnd
                    + " (" + saved.getReferenceCode() + "). Traiter : " + approvalLink;
            CompletableFuture.runAsync(() -> nextcloudTalkService.sendDirectMessage(talkId, msg));
        }

        return toSummaryDto(saved);
    }


    @Transactional(readOnly = true)
    public List<AbsenceSummaryDto> getUserAbsences(Long userId) {
        return absenceRequestRepository.findByUserIdWithDetails(userId)
                .stream()
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AbsenceSummaryDto getAbsenceById(Integer id) {
        AbsenceRequest request = absenceRequestRepository.findByIdWithAssociations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Absence request not found: " + id));
        return toSummaryDto(request);
    }

    @Transactional(readOnly = true)
    public List<AbsenceSummaryDto> getPendingAbsences(Long currentUserId, Set<String> roles) {
        List<AbsenceRequest> requests;
        if (roles.contains("ROLE_HR_MANAGER")) {
            requests = absenceRequestRepository.findByStatus(AbsenceStatus.PENDING_APPROVAL);
        } else {
            requests = absenceRequestRepository.findPendingByApprover(AbsenceStatus.PENDING_APPROVAL, currentUserId);
        }
        return requests.stream().map(this::toSummaryDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AbsenceSummaryDto> getApprovedAbsences(Long currentUserId, Set<String> roles) {
        List<AbsenceRequest> requests;
        if (roles.contains("ROLE_HR_MANAGER")) {
            requests = absenceRequestRepository.findByStatus(AbsenceStatus.APPROVED);
        } else {
            List<Long> supervisedTeamIds = getSupervisedTeamIds(currentUserId);
            if (!supervisedTeamIds.isEmpty()) {
                Set<AbsenceRequest> combined = new LinkedHashSet<>(
                        absenceRequestRepository.findByStatus(AbsenceStatus.APPROVED).stream()
                                .filter(r -> r.getApprovers() != null &&
                                        r.getApprovers().stream().anyMatch(u -> u.getId().equals(currentUserId)))
                                .toList());
                combined.addAll(absenceRequestRepository.findByStatusAndTeams(AbsenceStatus.APPROVED, supervisedTeamIds));
                requests = new ArrayList<>(combined);
            } else {
                requests = absenceRequestRepository.findByStatus(AbsenceStatus.APPROVED).stream()
                        .filter(r -> r.getApprovers() != null &&
                                r.getApprovers().stream().anyMatch(u -> u.getId().equals(currentUserId)))
                        .collect(Collectors.toList());
            }
        }
        return requests.stream().map(this::toSummaryDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AbsenceSummaryDto> getRejectedAbsences(Long currentUserId, Set<String> roles) {
        List<AbsenceRequest> requests;
        if (roles.contains("ROLE_HR_MANAGER")) {
            requests = absenceRequestRepository.findByStatus(AbsenceStatus.REJECTED);
            log.info("HR Manager {} retrieved {} rejected absences", currentUserId, requests.size());
        } else {
            List<Long> supervisedTeamIds = getSupervisedTeamIds(currentUserId);
            if (!supervisedTeamIds.isEmpty()) {
                Set<AbsenceRequest> combined = new LinkedHashSet<>(
                        absenceRequestRepository.findByStatus(AbsenceStatus.REJECTED).stream()
                                .filter(r -> r.getApprovers() != null &&
                                        r.getApprovers().stream().anyMatch(u -> u.getId().equals(currentUserId)))
                                .toList());
                combined.addAll(absenceRequestRepository.findByStatusAndTeams(AbsenceStatus.REJECTED, supervisedTeamIds));
                requests = new ArrayList<>(combined);
            } else {
                requests = absenceRequestRepository.findByStatus(AbsenceStatus.REJECTED).stream()
                        .filter(r -> r.getApprovers() != null &&
                                r.getApprovers().stream().anyMatch(u -> u.getId().equals(currentUserId)))
                        .collect(Collectors.toList());
            }
        }
        return requests.stream().map(this::toSummaryDto).collect(Collectors.toList());
    }

    private List<Long> getSupervisedTeamIds(Long userId) {
        return teamRepository.findSupervisedBy(userId)
                .stream().map(heritage.africca.accel_connect.entity.Team::getId).toList();
    }


    @Transactional
    public AbsenceSummaryDto updateAbsence(Integer id, Long userId, AbsenceRequestDto dto) {
        AbsenceRequest request = loadRequest(id);

        if (!request.getUser().getId().equals(userId)) {
            throw new ValidationException("Vous ne pouvez modifier que vos propres demandes d'absence.");
        }
        if (request.getStatus() != AbsenceStatus.PENDING_APPROVAL) {
            throw new ValidationException("Seules les demandes en attente peuvent être modifiées.");
        }

        validateDate(dto.getDate());
        validateTimes(dto.getStartTime(), dto.getEndTime());

        request.setAbsenceType(AbsenceTypeCode.fromJson(dto.getAbsenceType()));
        request.setDate(dto.getDate());
        request.setStartTime(dto.getStartTime());
        request.setEndTime(dto.getEndTime());
        request.setDurationHours(computeDuration(dto.getStartTime(), dto.getEndTime()));
        request.setReason(dto.getReason());
        request.setApprovers(resolveApprovers(dto.getApproverUserIds()));

        return toSummaryDto(absenceRequestRepository.save(request));
    }


    @Transactional
    public AbsenceSummaryDto approveAbsence(Integer id, AbsenceDecisionRequest decision,
                                             Long currentUserId, Set<String> roles) {
        AbsenceRequest request = loadRequest(id);
        validateDecisionAccess(request, currentUserId, roles);

        if (request.getStatus() != AbsenceStatus.PENDING_APPROVAL) {
            throw new ValidationException("Cette demande d'absence ne peut plus être approuvée (statut: " + request.getStatus().getDisplayName() + ").");
        }

        request.setStatus(AbsenceStatus.APPROVED);
        request.setDecidedBy(findUser(currentUserId));
        addComment(request, decision, currentUserId);
        AbsenceSummaryDto result = toSummaryDto(absenceRequestRepository.save(request));
        notificationService.createForUser(
                request.getUser().getId(),
                "Absence approuvée",
                "Votre demande d'absence " + request.getReferenceCode() + " a été approuvée.",
                frontendUrl + "/rh/absences"
        );
        emailService.notifyEmployeeOfAbsenceApproval(request);
        return result;
    }

    private void notifyObservers(AbsenceRequest saved, String employeeName) {
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
                "Nouvelle demande d'absence",
                employeeName + " a soumis une demande d'absence (" + saved.getReferenceCode() + ").",
                frontendUrl + "/rh/absences"
        ));
        emailService.notifyObserverOfNewAbsenceRequest(saved, observers);
    }

    @Transactional
    public AbsenceSummaryDto rejectAbsence(Integer id, AbsenceDecisionRequest decision,
                                            Long currentUserId, Set<String> roles) {
        AbsenceRequest request = loadRequest(id);
        validateDecisionAccess(request, currentUserId, roles);

        if (request.getStatus() != AbsenceStatus.PENDING_APPROVAL) {
            throw new ValidationException("Cette demande d'absence ne peut plus être rejetée (statut: " + request.getStatus().getDisplayName() + ").");
        }

        request.setStatus(AbsenceStatus.REJECTED);
        request.setDecidedBy(findUser(currentUserId));
        addComment(request, decision, currentUserId);
        AbsenceSummaryDto result = toSummaryDto(absenceRequestRepository.save(request));
        notificationService.createForUser(
                request.getUser().getId(),
                "Absence rejetée",
                "Votre demande d'absence " + request.getReferenceCode() + " a été rejetée.",
                frontendUrl + "/rh/absences"
        );
        emailService.notifyEmployeeOfAbsenceRejection(request);
        return result;
    }

    @Transactional
    public AbsenceSummaryDto cancelApprovedAbsence(Integer id, AbsenceDecisionRequest decision,
                                                   Long currentUserId, Set<String> roles) {
        AbsenceRequest request = loadRequest(id);
        validateDecisionAccess(request, currentUserId, roles);

        if (request.getStatus() != AbsenceStatus.APPROVED) {
            throw new ValidationException("Seules les absences approuvées peuvent être annulées (statut: " + request.getStatus().getDisplayName() + ").");
        }

        request.setStatus(AbsenceStatus.CANCELLED);
        request.setDecidedBy(findUser(currentUserId));
        addComment(request, decision, currentUserId);
        AbsenceSummaryDto result = toSummaryDto(absenceRequestRepository.save(request));
        notificationService.createForUser(
                request.getUser().getId(),
                "Absence annulée",
                "Votre demande d'absence " + request.getReferenceCode() + " a été annulée.",
                frontendUrl + "/rh/absences"
        );
        emailService.notifyEmployeeOfAbsenceCancellation(request);
        return result;
    }

    @Transactional
    public AbsenceSummaryDto cancelRejectedAbsence(Integer id, AbsenceDecisionRequest decision,
                                                   Long currentUserId, Set<String> roles) {
        AbsenceRequest request = loadRequest(id);
        validateDecisionAccess(request, currentUserId, roles);

        if (request.getStatus() != AbsenceStatus.REJECTED) {
            throw new ValidationException("Seules les absences rejetées peuvent être remises en attente (statut: " + request.getStatus().getDisplayName() + ").");
        }

        request.setStatus(AbsenceStatus.PENDING_APPROVAL);
        request.setDecidedBy(null);
        addComment(request, decision, currentUserId);
        AbsenceSummaryDto result = toSummaryDto(absenceRequestRepository.save(request));
        notificationService.createForUser(
                request.getUser().getId(),
                "Rejet annulé",
                "Votre demande d'absence " + request.getReferenceCode() + " est de nouveau en attente d'approbation.",
                frontendUrl + "/rh/absences"
        );
        return result;
    }

    @Transactional
    public void deleteAbsence(Integer id, Long userId) {
        AbsenceRequest request = loadRequest(id);
        if (!request.getUser().getId().equals(userId)) {
            throw new ValidationException("Vous ne pouvez supprimer que vos propres demandes d'absence.");
        }
        if (request.getStatus() != AbsenceStatus.PENDING_APPROVAL) {
            throw new ValidationException("Seules les demandes en attente peuvent être supprimées.");
        }
        absenceRequestRepository.delete(request);
    }

    private AbsenceRequest loadRequest(Integer id) {
        return absenceRequestRepository.findByIdWithAssociations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Absence request not found: " + id));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    private Set<User> resolveApprovers(List<Long> approverIds) {
        if (approverIds == null || approverIds.isEmpty()) return new HashSet<>();
        return approverIds.stream()
                .map(this::findUser)
                .collect(Collectors.toSet());
    }

    private void validateDate(LocalDate date) {
        if (date != null && date.isBefore(LocalDate.now())) {
            throw new ValidationException("La date d'absence ne peut pas être dans le passé.");
        }
    }

    private void validateTimes(LocalTime start, LocalTime end) {
        if (!end.isAfter(start)) {
            throw new ValidationException("L'heure de fin doit être après l'heure de début.");
        }
    }

    private BigDecimal computeDuration(LocalTime start, LocalTime end) {
        long minutes = java.time.Duration.between(start, end).toMinutes();
        return BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    private void validateDecisionAccess(AbsenceRequest request, Long currentUserId, Set<String> roles) {
        if (roles.contains("ROLE_HR_MANAGER")) return;
        boolean isApprover = request.getApprovers() != null &&
                request.getApprovers().stream().anyMatch(u -> u.getId().equals(currentUserId));
        if (!isApprover) {
            throw new ValidationException("Vous n'êtes pas autorisé à traiter cette demande d'absence.");
        }
    }

    private void addComment(AbsenceRequest request, AbsenceDecisionRequest decision, Long currentUserId) {
        if (decision == null || (decision.getComment() == null || decision.getComment().isBlank())) return;
        Long authorId = decision.getDecidedByUserId() != null ? decision.getDecidedByUserId() : currentUserId;
        User author = findUser(authorId);
        AbsenceComment comment = AbsenceComment.builder()
                .absenceRequest(request)
                .createdBy(author)
                .comments(decision.getComment())
                .build();
        if (request.getComments() == null) {
            request.setComments(new HashSet<>());
        }
        request.getComments().add(comment);
    }

    private String generateReferenceCode(LocalDate date) {
        String datePart = (date != null ? date : LocalDate.now()).format(REF_DATE_FMT);
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return "ABS-" + datePart + "-" + suffix;
    }

    private AbsenceSummaryDto toSummaryDto(AbsenceRequest r) {
        String employeeName = r.getUser() != null
                ? r.getUser().getFirstName() + " " + r.getUser().getLastName()
                : null;

        List<AbsenceSummaryDto.CommentDto> commentDtos = r.getComments() == null ? List.of() :
                r.getComments().stream().map(c -> AbsenceSummaryDto.CommentDto.builder()
                        .text(c.getComments())
                        .author(c.getCreatedBy() != null
                                ? c.getCreatedBy().getFirstName() + " " + c.getCreatedBy().getLastName()
                                : "")
                        .createdAt(c.getCreated())
                        .build()).collect(Collectors.toList());

        List<AbsenceSummaryDto.ApproverDto> approverDtos = r.getApprovers() == null ? List.of() :
                r.getApprovers().stream().map(u -> AbsenceSummaryDto.ApproverDto.builder()
                        .id(u.getId())
                        .displayName(u.getFirstName() + " " + u.getLastName())
                        .build()).collect(Collectors.toList());

        String decidedByName = r.getDecidedBy() != null
                ? r.getDecidedBy().getFirstName() + " " + r.getDecidedBy().getLastName()
                : null;

        return AbsenceSummaryDto.builder()
                .id(r.getId())
                .referenceCode(r.getReferenceCode())
                .employeeName(employeeName)
                .employeeEmail(r.getUser() != null ? r.getUser().getEmail() : null)
                .employeeUserId(r.getUser() != null ? r.getUser().getId() : null)
                .absenceTypeName(r.getAbsenceType() != null ? r.getAbsenceType().getDisplayName() : null)
                .date(r.getDate())
                .startTime(r.getStartTime())
                .endTime(r.getEndTime())
                .durationHours(r.getDurationHours())
                .reason(r.getReason())
                .status(r.getStatus() != null ? r.getStatus().getDisplayName() : null)
                .dateApplied(r.getDateApplied())
                .comments(commentDtos)
                .approvers(approverDtos)
                .decidedByName(decidedByName)
                .build();
    }
}

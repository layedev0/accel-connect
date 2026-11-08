package heritage.africca.accel_connect.service;

import heritage.africca.accel_connect.entity.AbsenceRequest;
import heritage.africca.accel_connect.entity.Leave;
import heritage.africca.accel_connect.entity.LeaveRequest;
import heritage.africca.accel_connect.entity.User;
import heritage.africca.accel_connect.entity.enumeration.AbsenceStatus;
import heritage.africca.accel_connect.repository.AbsenceRequestRepository;
import heritage.africca.accel_connect.repository.LeaveRequestRepository;
import heritage.africca.accel_connect.service.Impl.NextcloudTalkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class LeaveReminderScheduler {

    private final LeaveRequestRepository leaveRequestRepository;
    private final AbsenceRequestRepository absenceRequestRepository;
    private final EmailService emailService;
    private final NextcloudTalkService nextcloudTalkService;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Every day at 09:00 — send one digest reminder per approver for all pending
     * leave and absence requests that are still awaiting their decision.
     */
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional(readOnly = true)
    public void sendDailyPendingReminders() {
        log.info("Daily pending-approval reminder job started");
        sendLeaveReminders();
        sendAbsenceReminders();
        log.info("Daily pending-approval reminder job finished");
    }

    // --- Leave ---

    private void sendLeaveReminders() {
        List<LeaveRequest> pending = leaveRequestRepository.findAllWithPendingLeaves();
        if (pending.isEmpty()) return;

        String approvalUrl = frontendUrl + "/rh/approbation-conges";
        Map<User, List<LeaveRequest>> byApprover = groupLeavesByApprover(pending);

        for (Map.Entry<User, List<LeaveRequest>> entry : byApprover.entrySet()) {
            User approver = entry.getKey();
            List<LeaveRequest> requests = entry.getValue();
            log.info("Leave reminder → {} ({} pending)", approver.getEmail(), requests.size());

            emailService.sendPendingLeaveReminderToApprover(approver, requests, approvalUrl);

            if (approver.getKeycloakId() != null) {
                String talkId = "SSO-" + approver.getKeycloakId();
                String msg = buildLeaveNextcloudMsg(requests, approvalUrl);
                CompletableFuture.runAsync(() -> nextcloudTalkService.sendDirectMessage(talkId, msg));
            }
        }
    }

    private Map<User, List<LeaveRequest>> groupLeavesByApprover(List<LeaveRequest> requests) {
        Map<User, List<LeaveRequest>> map = new LinkedHashMap<>();
        for (LeaveRequest lr : requests) {
            if (lr.getApprovers() == null) continue;
            for (User approver : lr.getApprovers()) {
                if (approver == null || approver.getEmail() == null) continue;
                map.computeIfAbsent(approver, k -> new ArrayList<>()).add(lr);
            }
        }
        return map;
    }

    private String buildLeaveNextcloudMsg(List<LeaveRequest> requests, String approvalUrl) {
        StringBuilder sb = new StringBuilder();
        sb.append("⏰ Rappel — ").append(requests.size())
          .append(" demande(s) de congé en attente de votre validation :\n");
        for (LeaveRequest lr : requests) {
            String name = lr.getUser() != null
                    ? (lr.getUser().getFirstName() + " " + lr.getUser().getLastName()).trim() : "—";
            String start = lr.getLeaves() != null ? lr.getLeaves().stream()
                    .map(Leave::getDate).filter(Objects::nonNull)
                    .min(LocalDate::compareTo).map(d -> d.format(FMT)).orElse("—") : "—";
            String end = lr.getLeaves() != null ? lr.getLeaves().stream()
                    .map(Leave::getDate).filter(Objects::nonNull)
                    .max(LocalDate::compareTo).map(d -> d.format(FMT)).orElse("—") : "—";
            sb.append("  • #").append(lr.getId()).append(" — ").append(name)
              .append(" (").append(start).append(" → ").append(end).append(")\n");
        }
        sb.append("Traiter : ").append(approvalUrl);
        return sb.toString();
    }

    // --- Absence ---

    private void sendAbsenceReminders() {
        List<AbsenceRequest> pending = absenceRequestRepository.findByStatus(AbsenceStatus.PENDING_APPROVAL);
        if (pending.isEmpty()) return;

        String approvalUrl = frontendUrl + "/rh/approbation-absences";
        Map<User, List<AbsenceRequest>> byApprover = groupAbsencesByApprover(pending);

        for (Map.Entry<User, List<AbsenceRequest>> entry : byApprover.entrySet()) {
            User approver = entry.getKey();
            List<AbsenceRequest> absences = entry.getValue();
            log.info("Absence reminder → {} ({} pending)", approver.getEmail(), absences.size());

            emailService.sendPendingAbsenceReminderToApprover(approver, absences, approvalUrl);

            if (approver.getKeycloakId() != null) {
                String talkId = "SSO-" + approver.getKeycloakId();
                String msg = buildAbsenceNextcloudMsg(absences, approvalUrl);
                CompletableFuture.runAsync(() -> nextcloudTalkService.sendDirectMessage(talkId, msg));
            }
        }
    }

    private Map<User, List<AbsenceRequest>> groupAbsencesByApprover(List<AbsenceRequest> requests) {
        Map<User, List<AbsenceRequest>> map = new LinkedHashMap<>();
        for (AbsenceRequest ar : requests) {
            if (ar.getApprovers() == null) continue;
            for (User approver : ar.getApprovers()) {
                if (approver == null || approver.getEmail() == null) continue;
                map.computeIfAbsent(approver, k -> new ArrayList<>()).add(ar);
            }
        }
        return map;
    }

    private String buildAbsenceNextcloudMsg(List<AbsenceRequest> absences, String approvalUrl) {
        StringBuilder sb = new StringBuilder();
        sb.append("Rappel — ").append(absences.size())
          .append(" demande(s) d'absence en attente de votre validation :\n");
        for (AbsenceRequest ar : absences) {
            String name = ar.getUser() != null
                    ? (ar.getUser().getFirstName() + " " + ar.getUser().getLastName()).trim() : "—";
            String date = ar.getDate() != null ? ar.getDate().format(FMT) : "—";
            sb.append("  • ").append(ar.getReferenceCode()).append(" — ").append(name)
              .append(" (").append(date).append(")\n");
        }
        sb.append("Traiter : ").append(approvalUrl);
        return sb.toString();
    }
}

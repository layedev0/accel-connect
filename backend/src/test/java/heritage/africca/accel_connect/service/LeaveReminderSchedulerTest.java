package heritage.africca.accel_connect.service;

import heritage.africca.accel_connect.entity.AbsenceRequest;
import heritage.africca.accel_connect.entity.Leave;
import heritage.africca.accel_connect.entity.LeaveRequest;
import heritage.africca.accel_connect.entity.User;
import heritage.africca.accel_connect.entity.enumeration.AbsenceStatus;
import heritage.africca.accel_connect.entity.enumeration.AbsenceTypeCode;
import heritage.africca.accel_connect.entity.enumeration.LeaveStatus;
import heritage.africca.accel_connect.entity.enumeration.LeaveTypeCode;
import heritage.africca.accel_connect.repository.AbsenceRequestRepository;
import heritage.africca.accel_connect.repository.LeaveRequestRepository;
import heritage.africca.accel_connect.service.Impl.NextcloudTalkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LeaveReminderScheduler")
class LeaveReminderSchedulerTest {

    @Mock private LeaveRequestRepository leaveRequestRepository;
    @Mock private AbsenceRequestRepository absenceRequestRepository;
    @Mock private EmailService emailService;
    @Mock private NextcloudTalkService nextcloudTalkService;

    @InjectMocks
    private LeaveReminderScheduler scheduler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scheduler, "frontendUrl", "http://app.test");
    }



    private User approver(Long id, String firstName, String lastName, String email) {
        User u = new User();
        u.setId(id);
        u.setFirstName(firstName);
        u.setLastName(lastName);
        u.setEmail(email);
        return u;
    }

    private User approverWithNextcloud(Long id, String firstName, String lastName,
                                       String email, String keycloakId) {
        User u = approver(id, firstName, lastName, email);
        u.setKeycloakId(keycloakId);
        return u;
    }

    private User employee(Long id, String firstName, String lastName) {
        User u = new User();
        u.setId(id);
        u.setFirstName(firstName);
        u.setLastName(lastName);
        u.setEmail(firstName.toLowerCase() + "@example.com");
        return u;
    }

    private Leave pendingLeave(LocalDate date) {
        Leave l = new Leave();
        l.setDate(date);
        l.setStatus(LeaveStatus.PENDING_APPROVAL);
        l.setLengthDays(BigDecimal.ONE);
        l.setLengthHours(new BigDecimal("8"));
        return l;
    }

    private LeaveRequest pendingLeaveRequest(Integer id, User requester, List<User> approvers) {
        return LeaveRequest.builder()
                .id(id)
                .user(requester)
                .leaveType(LeaveTypeCode.ANNUAL_LEAVE)
                .approvers(approvers != null ? approvers : new ArrayList<>())
                .leaves(List.of(pendingLeave(LocalDate.of(2026, 6, 1))))
                .dateApplied(LocalDate.now())
                .comments(new ArrayList<>())
                .build();
    }

    private AbsenceRequest pendingAbsenceRequest(Integer id, User requester, Set<User> approvers) {
        return AbsenceRequest.builder()
                .id(id)
                .user(requester)
                .absenceType(AbsenceTypeCode.MALADE)
                .approvers(approvers != null ? approvers : new HashSet<>())
                .date(LocalDate.of(2026, 6, 10))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(12, 0))
                .durationHours(new BigDecimal("3"))
                .reason("Sick")
                .referenceCode("ABS-2026-00" + id)
                .status(AbsenceStatus.PENDING_APPROVAL)
                .comments(new HashSet<>())
                .build();
    }



    @Nested
    @DisplayName("leave reminders")
    class LeaveReminders {

        @Test
        @DisplayName("does nothing when no pending leave requests exist")
        void doesNothingWhenNoPendingLeaves() {
            scheduler.sendDailyPendingReminders();

            verifyNoInteractions(emailService, nextcloudTalkService);
        }

        @Test
        @DisplayName("sends one email to the approver when a single request is pending")
        void sendsOneEmailForSinglePendingRequest() {
            User approver = approver(1L, "Moussa", "Ndiaye", "moussa@example.com");
            LeaveRequest lr = pendingLeaveRequest(1, employee(10L, "Alice", "Diallo"), List.of(approver));
            when(leaveRequestRepository.findAllWithPendingLeaves()).thenReturn(List.of(lr));

            scheduler.sendDailyPendingReminders();

            verify(emailService).sendPendingLeaveReminderToApprover(
                    eq(approver), eq(List.of(lr)), eq("http://app.test/rh/approbation-conges"));
        }

        @Test
        @DisplayName("groups multiple pending requests under the same approver into one email")
        void groupsRequestsUnderSameApprover() {
            User approver = approver(1L, "Moussa", "Ndiaye", "moussa@example.com");
            LeaveRequest lr1 = pendingLeaveRequest(1, employee(10L, "Alice", "Diallo"), List.of(approver));
            LeaveRequest lr2 = pendingLeaveRequest(2, employee(11L, "Binta", "Sow"),   List.of(approver));
            when(leaveRequestRepository.findAllWithPendingLeaves()).thenReturn(List.of(lr1, lr2));

            scheduler.sendDailyPendingReminders();

            verify(emailService, times(1)).sendPendingLeaveReminderToApprover(
                    eq(approver),
                    argThat(list -> list.size() == 2 && list.contains(lr1) && list.contains(lr2)),
                    anyString());
        }

        @Test
        @DisplayName("sends separate emails for distinct approvers")
        void sendsOneEmailPerDistinctApprover() {
            User a1 = approver(1L, "Moussa", "Ndiaye", "moussa@example.com");
            User a2 = approver(2L, "Fatou",  "Sow",    "fatou@example.com");
            LeaveRequest lr1 = pendingLeaveRequest(1, employee(10L, "Alice", "Diallo"), List.of(a1));
            LeaveRequest lr2 = pendingLeaveRequest(2, employee(11L, "Binta", "Sow"),   List.of(a2));
            when(leaveRequestRepository.findAllWithPendingLeaves()).thenReturn(List.of(lr1, lr2));

            scheduler.sendDailyPendingReminders();

            verify(emailService).sendPendingLeaveReminderToApprover(eq(a1), anyList(), anyString());
            verify(emailService).sendPendingLeaveReminderToApprover(eq(a2), anyList(), anyString());
            verify(emailService, times(2)).sendPendingLeaveReminderToApprover(any(), anyList(), anyString());
        }

        @Test
        @DisplayName("skips leave request with empty approvers list")
        void skipsRequestWithNoApprovers() {
            LeaveRequest lr = pendingLeaveRequest(1, employee(10L, "Alice", "Diallo"), List.of());
            when(leaveRequestRepository.findAllWithPendingLeaves()).thenReturn(List.of(lr));

            scheduler.sendDailyPendingReminders();

            verifyNoInteractions(emailService);
        }

        @Test
        @DisplayName("skips approver with null email")
        void skipsApproverWithNullEmail() {
            User noEmail = approver(1L, "Moussa", "Ndiaye", null);
            LeaveRequest lr = pendingLeaveRequest(1, employee(10L, "Alice", "Diallo"), List.of(noEmail));
            when(leaveRequestRepository.findAllWithPendingLeaves()).thenReturn(List.of(lr));

            scheduler.sendDailyPendingReminders();

            verifyNoInteractions(emailService);
        }

        @Test
        @DisplayName("approval URL uses the configured frontend URL")
        void approvalUrlUsesConfiguredFrontendUrl() {
            User approver = approver(1L, "Moussa", "Ndiaye", "moussa@example.com");
            LeaveRequest lr = pendingLeaveRequest(1, employee(10L, "Alice", "Diallo"), List.of(approver));
            when(leaveRequestRepository.findAllWithPendingLeaves()).thenReturn(List.of(lr));

            scheduler.sendDailyPendingReminders();

            verify(emailService).sendPendingLeaveReminderToApprover(
                    any(), anyList(), eq("http://app.test/rh/approbation-conges"));
        }

        @Test
        @DisplayName("sends Nextcloud message when approver has a Keycloak ID")
        void sendsNextcloudWhenKeycloakIdPresent() {
            User approver = approverWithNextcloud(1L, "Moussa", "Ndiaye", "moussa@example.com", "kc-uuid-123");
            LeaveRequest lr = pendingLeaveRequest(1, employee(10L, "Alice", "Diallo"), List.of(approver));
            when(leaveRequestRepository.findAllWithPendingLeaves()).thenReturn(List.of(lr));

            scheduler.sendDailyPendingReminders();

            verify(nextcloudTalkService, timeout(1000))
                    .sendDirectMessage(eq("SSO-kc-uuid-123"), anyString());
        }

        @Test
        @DisplayName("skips Nextcloud when approver has no Keycloak ID")
        void skipsNextcloudWhenKeycloakIdIsNull() {
            User approver = approver(1L, "Moussa", "Ndiaye", "moussa@example.com");
            LeaveRequest lr = pendingLeaveRequest(1, employee(10L, "Alice", "Diallo"), List.of(approver));
            when(leaveRequestRepository.findAllWithPendingLeaves()).thenReturn(List.of(lr));

            scheduler.sendDailyPendingReminders();

            verifyNoInteractions(nextcloudTalkService);
        }

        @Test
        @DisplayName("Nextcloud message contains the approval URL")
        void nextcloudMessageContainsApprovalUrl() {
            User approver = approverWithNextcloud(1L, "Moussa", "Ndiaye", "moussa@example.com", "kc-uuid");
            LeaveRequest lr = pendingLeaveRequest(1, employee(10L, "Alice", "Diallo"), List.of(approver));
            when(leaveRequestRepository.findAllWithPendingLeaves()).thenReturn(List.of(lr));

            scheduler.sendDailyPendingReminders();

            verify(nextcloudTalkService, timeout(1000)).sendDirectMessage(
                    anyString(),
                    argThat(msg -> msg.contains("http://app.test/rh/approbation-conges")));
        }
    }



    @Nested
    @DisplayName("absence reminders")
    class AbsenceReminders {

        @Test
        @DisplayName("does nothing when no pending absence requests exist")
        void doesNothingWhenNoPendingAbsences() {
            // leaveRequestRepository and absenceRequestRepository both return empty by default
            scheduler.sendDailyPendingReminders();

            verifyNoInteractions(emailService, nextcloudTalkService);
        }

        @Test
        @DisplayName("sends one email to the approver when a single absence request is pending")
        void sendsOneEmailForSinglePendingAbsence() {
            User approver = approver(1L, "Moussa", "Ndiaye", "moussa@example.com");
            AbsenceRequest ar = pendingAbsenceRequest(1, employee(10L, "Alice", "Diallo"), Set.of(approver));
            when(absenceRequestRepository.findByStatus(AbsenceStatus.PENDING_APPROVAL)).thenReturn(List.of(ar));

            scheduler.sendDailyPendingReminders();

            verify(emailService).sendPendingAbsenceReminderToApprover(
                    eq(approver), eq(List.of(ar)), eq("http://app.test/rh/approbation-absences"));
        }

        @Test
        @DisplayName("groups multiple pending absence requests under the same approver")
        void groupsAbsencesUnderSameApprover() {
            User approver = approver(1L, "Moussa", "Ndiaye", "moussa@example.com");
            AbsenceRequest ar1 = pendingAbsenceRequest(1, employee(10L, "Alice", "Diallo"), Set.of(approver));
            AbsenceRequest ar2 = pendingAbsenceRequest(2, employee(11L, "Binta", "Sow"),   Set.of(approver));
            when(absenceRequestRepository.findByStatus(AbsenceStatus.PENDING_APPROVAL)).thenReturn(List.of(ar1, ar2));

            scheduler.sendDailyPendingReminders();

            verify(emailService, times(1)).sendPendingAbsenceReminderToApprover(
                    eq(approver),
                    argThat(list -> list.size() == 2),
                    anyString());
        }

        @Test
        @DisplayName("sends separate emails for distinct absence approvers")
        void sendsOneEmailPerDistinctAbsenceApprover() {
            User a1 = approver(1L, "Moussa", "Ndiaye", "moussa@example.com");
            User a2 = approver(2L, "Fatou",  "Sow",    "fatou@example.com");
            AbsenceRequest ar1 = pendingAbsenceRequest(1, employee(10L, "Alice", "Diallo"), Set.of(a1));
            AbsenceRequest ar2 = pendingAbsenceRequest(2, employee(11L, "Binta", "Sow"),   Set.of(a2));
            when(absenceRequestRepository.findByStatus(AbsenceStatus.PENDING_APPROVAL)).thenReturn(List.of(ar1, ar2));

            scheduler.sendDailyPendingReminders();

            verify(emailService).sendPendingAbsenceReminderToApprover(eq(a1), anyList(), anyString());
            verify(emailService).sendPendingAbsenceReminderToApprover(eq(a2), anyList(), anyString());
        }

        @Test
        @DisplayName("absence approval URL uses the configured frontend URL")
        void absenceApprovalUrlUsesConfiguredFrontendUrl() {
            User approver = approver(1L, "Moussa", "Ndiaye", "moussa@example.com");
            AbsenceRequest ar = pendingAbsenceRequest(1, employee(10L, "Alice", "Diallo"), Set.of(approver));
            when(absenceRequestRepository.findByStatus(AbsenceStatus.PENDING_APPROVAL)).thenReturn(List.of(ar));

            scheduler.sendDailyPendingReminders();

            verify(emailService).sendPendingAbsenceReminderToApprover(
                    any(), anyList(), eq("http://app.test/rh/approbation-absences"));
        }

        @Test
        @DisplayName("sends Nextcloud message for absence when approver has a Keycloak ID")
        void sendsNextcloudForAbsenceWhenKeycloakIdPresent() {
            User approver = approverWithNextcloud(1L, "Moussa", "Ndiaye", "moussa@example.com", "kc-uuid-456");
            AbsenceRequest ar = pendingAbsenceRequest(1, employee(10L, "Alice", "Diallo"), Set.of(approver));
            when(absenceRequestRepository.findByStatus(AbsenceStatus.PENDING_APPROVAL)).thenReturn(List.of(ar));

            scheduler.sendDailyPendingReminders();

            verify(nextcloudTalkService, timeout(1000))
                    .sendDirectMessage(eq("SSO-kc-uuid-456"), anyString());
        }

        @Test
        @DisplayName("absence Nextcloud message contains the approval URL")
        void absenceNextcloudMessageContainsApprovalUrl() {
            User approver = approverWithNextcloud(1L, "Moussa", "Ndiaye", "moussa@example.com", "kc-uuid");
            AbsenceRequest ar = pendingAbsenceRequest(1, employee(10L, "Alice", "Diallo"), Set.of(approver));
            when(absenceRequestRepository.findByStatus(AbsenceStatus.PENDING_APPROVAL)).thenReturn(List.of(ar));

            scheduler.sendDailyPendingReminders();

            verify(nextcloudTalkService, timeout(1000)).sendDirectMessage(
                    anyString(),
                    argThat(msg -> msg.contains("http://app.test/rh/approbation-absences")));
        }
    }


    @Nested
    @DisplayName("combined leave and absence reminders")
    class Combined {

        @Test
        @DisplayName("sends both leave and absence reminders in the same run")
        void sendsBothLeaveAndAbsenceReminders() {
            User leaveApprover   = approver(1L, "Moussa", "Ndiaye", "moussa@example.com");
            User absenceApprover = approver(2L, "Fatou",  "Sow",    "fatou@example.com");

            LeaveRequest lr = pendingLeaveRequest(1, employee(10L, "Alice", "Diallo"), List.of(leaveApprover));
            AbsenceRequest ar = pendingAbsenceRequest(1, employee(11L, "Binta", "Sow"), Set.of(absenceApprover));

            when(leaveRequestRepository.findAllWithPendingLeaves()).thenReturn(List.of(lr));
            when(absenceRequestRepository.findByStatus(AbsenceStatus.PENDING_APPROVAL)).thenReturn(List.of(ar));

            scheduler.sendDailyPendingReminders();

            verify(emailService).sendPendingLeaveReminderToApprover(eq(leaveApprover), anyList(), anyString());
            verify(emailService).sendPendingAbsenceReminderToApprover(eq(absenceApprover), anyList(), anyString());
        }
    }
}

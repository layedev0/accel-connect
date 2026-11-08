package heritage.africca.accel_connect.service;

import heritage.africca.accel_connect.entity.AbsenceRequest;
import heritage.africca.accel_connect.entity.Leave;
import heritage.africca.accel_connect.entity.LeaveRequest;
import heritage.africca.accel_connect.entity.User;
import heritage.africca.accel_connect.entity.enumeration.AbsenceStatus;
import heritage.africca.accel_connect.entity.enumeration.AbsenceTypeCode;
import heritage.africca.accel_connect.entity.enumeration.LeaveStatus;
import heritage.africca.accel_connect.entity.enumeration.LeaveTypeCode;
import jakarta.mail.Message;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService")
class EmailServiceTest {

    @Mock  private JavaMailSender mailSender;
    @InjectMocks private EmailService emailService;
    @Captor private ArgumentCaptor<MimeMessage> messageCaptor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromAddress", "test@accel-connect.com");
    }

    // ── shared helpers ────────────────────────────────────────────────────────

    private MimeMessage newMimeMessage() {
        return new MimeMessage((jakarta.mail.Session) null);
    }

    private User makeApprover(Long id, String firstName, String lastName, String email) {
        User u = new User();
        u.setId(id);
        u.setFirstName(firstName);
        u.setLastName(lastName);
        u.setEmail(email);
        return u;
    }

    private User makeRequester() {
        User u = new User();
        u.setId(10L);
        u.setFirstName("Alice");
        u.setLastName("Diallo");
        u.setEmail("alice.diallo@example.com");
        return u;
    }

    /** Reads the HTML body from a multipart MimeMessage. */
    private String extractHtmlBody(MimeMessage mime) throws Exception {
        Object mixed = mime.getContent();
        Object relatedPart = mixed.getClass().getMethod("getBodyPart", int.class).invoke(mixed, 0);
        Object related    = relatedPart.getClass().getMethod("getContent").invoke(relatedPart);
        Object htmlPart   = related.getClass().getMethod("getBodyPart", int.class).invoke(related, 0);
        return htmlPart.getClass().getMethod("getContent").invoke(htmlPart).toString();
    }

    // ── leave helpers ─────────────────────────────────────────────────────────

    private Leave makeLeave(LocalDate date) {
        Leave leave = new Leave();
        leave.setDate(date);
        leave.setStatus(LeaveStatus.PENDING_APPROVAL);
        return leave;
    }

    private LeaveRequest buildLeaveRequest(User requester, LeaveTypeCode leaveType,
                                           List<User> approvers, List<Leave> leaves) {
        return LeaveRequest.builder()
                .id(99)
                .user(requester)
                .leaveType(leaveType)
                .approvers(approvers != null ? approvers : new ArrayList<>())
                .leaves(leaves != null ? leaves : new ArrayList<>())
                .build();
    }

    // ── absence helpers ───────────────────────────────────────────────────────

    private AbsenceRequest buildAbsenceRequest(User requester, AbsenceTypeCode absenceType,
                                               Set<User> approvers, LocalDate date,
                                               LocalTime start, LocalTime end,
                                               String referenceCode) {
        return AbsenceRequest.builder()
                .id(42)
                .user(requester)
                .absenceType(absenceType)
                .approvers(approvers != null ? approvers : new HashSet<>())
                .date(date)
                .startTime(start)
                .endTime(end)
                .referenceCode(referenceCode)
                .status(AbsenceStatus.PENDING_APPROVAL)
                .comments(new HashSet<>())
                .build();
    }

    private AbsenceRequest defaultAbsenceRequest(Set<User> approvers) {
        return buildAbsenceRequest(
                makeRequester(), AbsenceTypeCode.MALADE, approvers,
                LocalDate.of(2026, 4, 10),
                LocalTime.of(9, 0), LocalTime.of(12, 0),
                "ABS-20260410-A1B2C3");
    }

    @Nested
    @DisplayName("notifyApproversOfNewLeaveRequest")
    class NotifyApprovers {

        @Test
        @DisplayName("does nothing when approvers list is null")
        void doesNothingWhenApproversNull() {
            LeaveRequest req = buildLeaveRequest(makeRequester(), LeaveTypeCode.ANNUAL_LEAVE, null, null);

            emailService.notifyApproversOfNewLeaveRequest(req);

            verifyNoInteractions(mailSender);
        }

        @Test
        @DisplayName("does nothing when approvers list is empty")
        void doesNothingWhenApproversEmpty() {
            LeaveRequest req = buildLeaveRequest(makeRequester(), LeaveTypeCode.ANNUAL_LEAVE, List.of(), null);

            emailService.notifyApproversOfNewLeaveRequest(req);

            verifyNoInteractions(mailSender);
        }

        @Test
        @DisplayName("sends one email per approver")
        void sendsOneEmailPerApprover() {
            User a1 = makeApprover(1L, "Moussa", "Ndiaye", "moussa@example.com");
            User a2 = makeApprover(2L, "Fatou",  "Sow",    "fatou@example.com");
            LeaveRequest req = buildLeaveRequest(makeRequester(), LeaveTypeCode.ANNUAL_LEAVE,
                    List.of(a1, a2), List.of(makeLeave(LocalDate.now().plusDays(5))));

            when(mailSender.createMimeMessage()).thenReturn(newMimeMessage(), newMimeMessage());

            emailService.notifyApproversOfNewLeaveRequest(req);

            verify(mailSender, times(2)).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("skips approver with null email — no message sent")
        void skipsApproverWithNullEmail() {
            User noEmail = makeApprover(1L, "Moussa", "Ndiaye", null);
            LeaveRequest req = buildLeaveRequest(makeRequester(), LeaveTypeCode.ANNUAL_LEAVE,
                    List.of(noEmail), null);

            emailService.notifyApproversOfNewLeaveRequest(req);

            verifyNoInteractions(mailSender);
        }

        @Test
        @DisplayName("skips null approver entry without throwing")
        void skipsNullApproverEntry() {
            User valid = makeApprover(2L, "Fatou", "Sow", "fatou@example.com");
            List<User> approvers = new ArrayList<>();
            approvers.add(null);
            approvers.add(valid);
            LeaveRequest req = buildLeaveRequest(makeRequester(), LeaveTypeCode.ANNUAL_LEAVE, approvers, null);
            when(mailSender.createMimeMessage()).thenReturn(newMimeMessage());

            assertDoesNotThrow(() -> emailService.notifyApproversOfNewLeaveRequest(req));
            verify(mailSender, times(1)).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("continues sending to remaining approvers when one send fails")
        void continuesAfterSingleSendFailure() {
            User a1 = makeApprover(1L, "Moussa", "Ndiaye", "moussa@example.com");
            User a2 = makeApprover(2L, "Fatou",  "Sow",    "fatou@example.com");
            LeaveRequest req = buildLeaveRequest(makeRequester(), LeaveTypeCode.ANNUAL_LEAVE,
                    List.of(a1, a2), null);

            when(mailSender.createMimeMessage()).thenReturn(newMimeMessage(), newMimeMessage());
            doThrow(new org.springframework.mail.MailSendException("SMTP error"))
                    .doNothing().when(mailSender).send(any(MimeMessage.class));

            assertDoesNotThrow(() -> emailService.notifyApproversOfNewLeaveRequest(req));
            verify(mailSender, times(2)).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("email subject contains the requester's full name")
        void subjectContainsRequesterName() throws Exception {
            User approver = makeApprover(1L, "Moussa", "Ndiaye", "moussa@example.com");
            LeaveRequest req = buildLeaveRequest(makeRequester(), LeaveTypeCode.ANNUAL_LEAVE,
                    List.of(approver), null);
            MimeMessage mime = newMimeMessage();
            when(mailSender.createMimeMessage()).thenReturn(mime);

            emailService.notifyApproversOfNewLeaveRequest(req);

            verify(mailSender).send(messageCaptor.capture());
            String subject = messageCaptor.getValue().getSubject();
            assertNotNull(subject);
            assertTrue(subject.contains("Alice Diallo"),
                    "Subject should contain requester name, was: " + subject);
        }

        @Test
        @DisplayName("email is addressed to the approver's email")
        void recipientIsApproverEmail() throws Exception {
            User approver = makeApprover(1L, "Moussa", "Ndiaye", "moussa@example.com");
            LeaveRequest req = buildLeaveRequest(makeRequester(), LeaveTypeCode.ANNUAL_LEAVE,
                    List.of(approver), null);
            MimeMessage mime = newMimeMessage();
            when(mailSender.createMimeMessage()).thenReturn(mime);

            emailService.notifyApproversOfNewLeaveRequest(req);

            verify(mailSender).send(messageCaptor.capture());
            String[] recipients = Arrays.stream(
                            messageCaptor.getValue().getRecipients(Message.RecipientType.TO))
                    .map(jakarta.mail.Address::toString)
                    .toArray(String[]::new);
            assertTrue(Arrays.stream(recipients).anyMatch(r -> r.contains("moussa@example.com")));
        }

        @Test
        @DisplayName("falls back to 'Un employé' when leave request has no user")
        void fallbackRequesterNameWhenUserIsNull() throws Exception {
            User approver = makeApprover(1L, "Moussa", "Ndiaye", "moussa@example.com");
            LeaveRequest req = buildLeaveRequest(null, LeaveTypeCode.ANNUAL_LEAVE, List.of(approver), null);
            MimeMessage mime = newMimeMessage();
            when(mailSender.createMimeMessage()).thenReturn(mime);

            emailService.notifyApproversOfNewLeaveRequest(req);

            verify(mailSender).send(messageCaptor.capture());
            assertTrue(messageCaptor.getValue().getSubject().contains("Un employé"));
        }

        @Test
        @DisplayName("falls back to 'Congé' when leave type is null — still sends")
        void fallbackLeaveTypeNameWhenNull() {
            User approver = makeApprover(1L, "Moussa", "Ndiaye", "moussa@example.com");
            LeaveRequest req = buildLeaveRequest(makeRequester(), null, List.of(approver), null);
            when(mailSender.createMimeMessage()).thenReturn(newMimeMessage());

            assertDoesNotThrow(() -> emailService.notifyApproversOfNewLeaveRequest(req));
            verify(mailSender, times(1)).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("handles null leaves list — dates fall back to '—'")
        void handlesNullLeavesListGracefully() {
            User approver = makeApprover(1L, "Moussa", "Ndiaye", "moussa@example.com");
            LeaveRequest req = buildLeaveRequest(makeRequester(), LeaveTypeCode.ANNUAL_LEAVE,
                    List.of(approver), null);
            when(mailSender.createMimeMessage()).thenReturn(newMimeMessage());

            assertDoesNotThrow(() -> emailService.notifyApproversOfNewLeaveRequest(req));
            verify(mailSender, times(1)).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("email body contains correct start and end dates derived from leaves")
        void derivesCorrectDatesFromLeaves() throws Exception {
            User approver = makeApprover(1L, "Moussa", "Ndiaye", "moussa@example.com");
            LocalDate d1 = LocalDate.of(2026, 4, 7);
            LocalDate d2 = LocalDate.of(2026, 4, 10);
            LeaveRequest req = buildLeaveRequest(makeRequester(), LeaveTypeCode.ANNUAL_LEAVE,
                    List.of(approver), List.of(makeLeave(d2), makeLeave(d1)));
            MimeMessage mime = newMimeMessage();
            when(mailSender.createMimeMessage()).thenReturn(mime);

            emailService.notifyApproversOfNewLeaveRequest(req);

            verify(mailSender).send(messageCaptor.capture());
            String body = extractHtmlBody(messageCaptor.getValue());
            assertTrue(body.contains("07/04/2026"), "Should contain start date 07/04/2026");
            assertTrue(body.contains("10/04/2026"), "Should contain end date 10/04/2026");
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // notifyApproversOfNewAbsenceRequest
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("notifyApproversOfNewAbsenceRequest")
    class NotifyAbsenceApprovers {

        @Test
        @DisplayName("does nothing when approvers set is null")
        void doesNothingWhenApproversNull() {
            AbsenceRequest req = buildAbsenceRequest(
                    makeRequester(), AbsenceTypeCode.MALADE, null,
                    LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(12, 0),
                    "ABS-20260410-ABC123");

            emailService.notifyApproversOfNewAbsenceRequest(req);

            verifyNoInteractions(mailSender);
        }

        @Test
        @DisplayName("does nothing when approvers set is empty")
        void doesNothingWhenApproversEmpty() {
            AbsenceRequest req = defaultAbsenceRequest(new HashSet<>());

            emailService.notifyApproversOfNewAbsenceRequest(req);

            verifyNoInteractions(mailSender);
        }

        @Test
        @DisplayName("sends one email per approver")
        void sendsOneEmailPerApprover() {
            User a1 = makeApprover(1L, "Moussa", "Ndiaye", "moussa@example.com");
            User a2 = makeApprover(2L, "Fatou",  "Sow",    "fatou@example.com");
            AbsenceRequest req = defaultAbsenceRequest(new HashSet<>(Set.of(a1, a2)));
            when(mailSender.createMimeMessage()).thenReturn(newMimeMessage(), newMimeMessage());

            emailService.notifyApproversOfNewAbsenceRequest(req);

            verify(mailSender, times(2)).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("skips approver with null email — no message sent")
        void skipsApproverWithNullEmail() {
            User noEmail = makeApprover(1L, "Moussa", "Ndiaye", null);
            AbsenceRequest req = defaultAbsenceRequest(new HashSet<>(Set.of(noEmail)));

            emailService.notifyApproversOfNewAbsenceRequest(req);

            verifyNoInteractions(mailSender);
        }

        @Test
        @DisplayName("continues sending to remaining approvers when one send fails")
        void continuesAfterSingleSendFailure() {
            User a1 = makeApprover(1L, "Moussa", "Ndiaye", "moussa@example.com");
            User a2 = makeApprover(2L, "Fatou",  "Sow",    "fatou@example.com");
            AbsenceRequest req = defaultAbsenceRequest(new HashSet<>(Set.of(a1, a2)));
            when(mailSender.createMimeMessage()).thenReturn(newMimeMessage(), newMimeMessage());
            doThrow(new org.springframework.mail.MailSendException("SMTP error"))
                    .doNothing().when(mailSender).send(any(MimeMessage.class));

            assertDoesNotThrow(() -> emailService.notifyApproversOfNewAbsenceRequest(req));
            verify(mailSender, times(2)).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("email subject contains the requester's full name")
        void subjectContainsRequesterName() throws Exception {
            User approver = makeApprover(1L, "Moussa", "Ndiaye", "moussa@example.com");
            AbsenceRequest req = defaultAbsenceRequest(new HashSet<>(Set.of(approver)));
            MimeMessage mime = newMimeMessage();
            when(mailSender.createMimeMessage()).thenReturn(mime);

            emailService.notifyApproversOfNewAbsenceRequest(req);

            verify(mailSender).send(messageCaptor.capture());
            String subject = messageCaptor.getValue().getSubject();
            assertNotNull(subject);
            assertTrue(subject.contains("Alice Diallo"),
                    "Subject should contain requester name, was: " + subject);
        }

        @Test
        @DisplayName("email is addressed to the approver's email")
        void recipientIsApproverEmail() throws Exception {
            User approver = makeApprover(1L, "Moussa", "Ndiaye", "moussa@example.com");
            AbsenceRequest req = defaultAbsenceRequest(new HashSet<>(Set.of(approver)));
            MimeMessage mime = newMimeMessage();
            when(mailSender.createMimeMessage()).thenReturn(mime);

            emailService.notifyApproversOfNewAbsenceRequest(req);

            verify(mailSender).send(messageCaptor.capture());
            String[] recipients = Arrays.stream(
                            messageCaptor.getValue().getRecipients(Message.RecipientType.TO))
                    .map(jakarta.mail.Address::toString)
                    .toArray(String[]::new);
            assertTrue(Arrays.stream(recipients).anyMatch(r -> r.contains("moussa@example.com")));
        }

        @Test
        @DisplayName("falls back to 'Un employé' when absence request has no user")
        void fallbackRequesterNameWhenUserIsNull() throws Exception {
            User approver = makeApprover(1L, "Moussa", "Ndiaye", "moussa@example.com");
            AbsenceRequest req = buildAbsenceRequest(
                    null, AbsenceTypeCode.MALADE, new HashSet<>(Set.of(approver)),
                    LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(12, 0),
                    "ABS-20260410-ABC123");
            when(mailSender.createMimeMessage()).thenReturn(newMimeMessage());

            emailService.notifyApproversOfNewAbsenceRequest(req);

            verify(mailSender).send(messageCaptor.capture());
            assertTrue(messageCaptor.getValue().getSubject().contains("Un employé"));
        }

        @Test
        @DisplayName("falls back gracefully when absence type is null — still sends")
        void fallbackAbsenceTypeNameWhenNull() {
            User approver = makeApprover(1L, "Moussa", "Ndiaye", "moussa@example.com");
            AbsenceRequest req = buildAbsenceRequest(
                    makeRequester(), null, new HashSet<>(Set.of(approver)),
                    LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(12, 0),
                    "ABS-20260410-ABC123");
            when(mailSender.createMimeMessage()).thenReturn(newMimeMessage());

            assertDoesNotThrow(() -> emailService.notifyApproversOfNewAbsenceRequest(req));
            verify(mailSender, times(1)).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("handles null date and times without throwing")
        void handlesNullDateAndTimes() {
            User approver = makeApprover(1L, "Moussa", "Ndiaye", "moussa@example.com");
            AbsenceRequest req = buildAbsenceRequest(
                    makeRequester(), AbsenceTypeCode.MALADE, new HashSet<>(Set.of(approver)),
                    null, null, null, "ABS-20260410-ABC123");
            when(mailSender.createMimeMessage()).thenReturn(newMimeMessage());

            assertDoesNotThrow(() -> emailService.notifyApproversOfNewAbsenceRequest(req));
            verify(mailSender, times(1)).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("email body contains the reference code")
        void bodyContainsReferenceCode() throws Exception {
            User approver = makeApprover(1L, "Moussa", "Ndiaye", "moussa@example.com");
            AbsenceRequest req = defaultAbsenceRequest(new HashSet<>(Set.of(approver)));
            MimeMessage mime = newMimeMessage();
            when(mailSender.createMimeMessage()).thenReturn(mime);

            emailService.notifyApproversOfNewAbsenceRequest(req);

            verify(mailSender).send(messageCaptor.capture());
            String body = extractHtmlBody(messageCaptor.getValue());
            assertTrue(body.contains("ABS-20260410-A1B2C3"),
                    "Body should contain reference code, was: " + body);
        }

        @Test
        @DisplayName("email body contains the formatted date")
        void bodyContainsFormattedDate() throws Exception {
            User approver = makeApprover(1L, "Moussa", "Ndiaye", "moussa@example.com");
            AbsenceRequest req = defaultAbsenceRequest(new HashSet<>(Set.of(approver)));
            MimeMessage mime = newMimeMessage();
            when(mailSender.createMimeMessage()).thenReturn(mime);

            emailService.notifyApproversOfNewAbsenceRequest(req);

            verify(mailSender).send(messageCaptor.capture());
            String body = extractHtmlBody(messageCaptor.getValue());
            assertTrue(body.contains("10/04/2026"),
                    "Body should contain formatted date 10/04/2026, was: " + body);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // sendPendingLeaveReminderToApprover  (daily reminder digest)
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("sendPendingLeaveReminderToApprover")
    class SendPendingLeaveReminder {

        @Test
        @DisplayName("does nothing when approver is null")
        void doesNothingWhenApproverIsNull() {
            emailService.sendPendingLeaveReminderToApprover(null, List.of(), "http://app/conges");

            verifyNoInteractions(mailSender);
        }

        @Test
        @DisplayName("does nothing when approver email is null")
        void doesNothingWhenApproverEmailIsNull() {
            User approver = makeApprover(1L, "Moussa", "Ndiaye", null);

            emailService.sendPendingLeaveReminderToApprover(approver, List.of(), "http://app/conges");

            verifyNoInteractions(mailSender);
        }

        @Test
        @DisplayName("does nothing when pending list is empty")
        void doesNothingWhenPendingListIsEmpty() {
            User approver = makeApprover(1L, "Moussa", "Ndiaye", "moussa@example.com");

            emailService.sendPendingLeaveReminderToApprover(approver, List.of(), "http://app/conges");

            verifyNoInteractions(mailSender);
        }

        @Test
        @DisplayName("sends exactly one email to the approver")
        void sendsOneEmailToApprover() {
            User approver = makeApprover(1L, "Moussa", "Ndiaye", "moussa@example.com");
            LeaveRequest lr = buildLeaveRequest(makeRequester(), LeaveTypeCode.ANNUAL_LEAVE,
                    List.of(approver), List.of(makeLeave(LocalDate.of(2026, 6, 1))));
            when(mailSender.createMimeMessage()).thenReturn(newMimeMessage());

            emailService.sendPendingLeaveReminderToApprover(approver, List.of(lr), "http://app/conges");

            verify(mailSender, times(1)).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("email is addressed to the approver")
        void recipientIsApprover() throws Exception {
            User approver = makeApprover(1L, "Moussa", "Ndiaye", "moussa@example.com");
            LeaveRequest lr = buildLeaveRequest(makeRequester(), LeaveTypeCode.ANNUAL_LEAVE,
                    List.of(approver), List.of(makeLeave(LocalDate.of(2026, 6, 1))));
            MimeMessage mime = newMimeMessage();
            when(mailSender.createMimeMessage()).thenReturn(mime);

            emailService.sendPendingLeaveReminderToApprover(approver, List.of(lr), "http://app/conges");

            verify(mailSender).send(messageCaptor.capture());
            String recipient = Arrays.stream(messageCaptor.getValue()
                            .getRecipients(jakarta.mail.Message.RecipientType.TO))
                    .map(jakarta.mail.Address::toString).findFirst().orElse("");
            assertTrue(recipient.contains("moussa@example.com"));
        }

        @Test
        @DisplayName("email subject contains the pending count")
        void subjectContainsPendingCount() throws Exception {
            User approver = makeApprover(1L, "Moussa", "Ndiaye", "moussa@example.com");
            LeaveRequest lr1 = buildLeaveRequest(makeRequester(), LeaveTypeCode.ANNUAL_LEAVE,
                    List.of(approver), List.of(makeLeave(LocalDate.of(2026, 6, 1))));
            LeaveRequest lr2 = buildLeaveRequest(makeRequester(), LeaveTypeCode.ANNUAL_LEAVE,
                    List.of(approver), List.of(makeLeave(LocalDate.of(2026, 7, 1))));
            MimeMessage mime = newMimeMessage();
            when(mailSender.createMimeMessage()).thenReturn(mime);

            emailService.sendPendingLeaveReminderToApprover(approver, List.of(lr1, lr2), "http://app");

            verify(mailSender).send(messageCaptor.capture());
            assertTrue(messageCaptor.getValue().getSubject().contains("2"),
                    "Subject should mention count 2");
        }

        @Test
        @DisplayName("email body contains employee name")
        void bodyContainsEmployeeName() throws Exception {
            User approver = makeApprover(1L, "Moussa", "Ndiaye", "moussa@example.com");
            LeaveRequest lr = buildLeaveRequest(makeRequester(), LeaveTypeCode.ANNUAL_LEAVE,
                    List.of(approver), List.of(makeLeave(LocalDate.of(2026, 6, 1))));
            MimeMessage mime = newMimeMessage();
            when(mailSender.createMimeMessage()).thenReturn(mime);

            emailService.sendPendingLeaveReminderToApprover(approver, List.of(lr), "http://app");

            verify(mailSender).send(messageCaptor.capture());
            String body = extractHtmlBody(messageCaptor.getValue());
            assertTrue(body.contains("Alice Diallo"), "Body should contain employee name");
        }

        @Test
        @DisplayName("email body contains leave dates derived from leaves collection")
        void bodyContainsLeaveDates() throws Exception {
            User approver = makeApprover(1L, "Moussa", "Ndiaye", "moussa@example.com");
            LocalDate d1 = LocalDate.of(2026, 6, 1);
            LocalDate d2 = LocalDate.of(2026, 6, 5);
            LeaveRequest lr = buildLeaveRequest(makeRequester(), LeaveTypeCode.ANNUAL_LEAVE,
                    List.of(approver), List.of(makeLeave(d2), makeLeave(d1)));
            MimeMessage mime = newMimeMessage();
            when(mailSender.createMimeMessage()).thenReturn(mime);

            emailService.sendPendingLeaveReminderToApprover(approver, List.of(lr), "http://app");

            verify(mailSender).send(messageCaptor.capture());
            String body = extractHtmlBody(messageCaptor.getValue());
            assertTrue(body.contains("01/06/2026"), "Should contain start date");
            assertTrue(body.contains("05/06/2026"), "Should contain end date");
        }

        @Test
        @DisplayName("email body contains the approval URL")
        void bodyContainsApprovalUrl() throws Exception {
            User approver = makeApprover(1L, "Moussa", "Ndiaye", "moussa@example.com");
            LeaveRequest lr = buildLeaveRequest(makeRequester(), LeaveTypeCode.ANNUAL_LEAVE,
                    List.of(approver), List.of(makeLeave(LocalDate.of(2026, 6, 1))));
            MimeMessage mime = newMimeMessage();
            when(mailSender.createMimeMessage()).thenReturn(mime);

            emailService.sendPendingLeaveReminderToApprover(approver, List.of(lr), "http://app/conges");

            verify(mailSender).send(messageCaptor.capture());
            String body = extractHtmlBody(messageCaptor.getValue());
            assertTrue(body.contains("http://app/conges"), "Body should contain approval URL");
        }

        @Test
        @DisplayName("handles null leaves list without throwing")
        void handlesNullLeavesListGracefully() {
            User approver = makeApprover(1L, "Moussa", "Ndiaye", "moussa@example.com");
            LeaveRequest lr = buildLeaveRequest(makeRequester(), LeaveTypeCode.ANNUAL_LEAVE,
                    List.of(approver), null);
            when(mailSender.createMimeMessage()).thenReturn(newMimeMessage());

            assertDoesNotThrow(() ->
                    emailService.sendPendingLeaveReminderToApprover(approver, List.of(lr), "http://app"));
            verify(mailSender, times(1)).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("does not propagate exception when mail sender fails")
        void doesNotThrowWhenMailSenderFails() {
            User approver = makeApprover(1L, "Moussa", "Ndiaye", "moussa@example.com");
            LeaveRequest lr = buildLeaveRequest(makeRequester(), LeaveTypeCode.ANNUAL_LEAVE,
                    List.of(approver), null);
            when(mailSender.createMimeMessage()).thenReturn(newMimeMessage());
            doThrow(new org.springframework.mail.MailSendException("SMTP error"))
                    .when(mailSender).send(any(MimeMessage.class));

            assertDoesNotThrow(() ->
                    emailService.sendPendingLeaveReminderToApprover(approver, List.of(lr), "http://app"));
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // sendPendingAbsenceReminderToApprover  (daily reminder digest)
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("sendPendingAbsenceReminderToApprover")
    class SendPendingAbsenceReminder {

        @Test
        @DisplayName("does nothing when approver is null")
        void doesNothingWhenApproverIsNull() {
            emailService.sendPendingAbsenceReminderToApprover(null, List.of(), "http://app/absences");

            verifyNoInteractions(mailSender);
        }

        @Test
        @DisplayName("does nothing when approver email is null")
        void doesNothingWhenApproverEmailIsNull() {
            User approver = makeApprover(1L, "Moussa", "Ndiaye", null);

            emailService.sendPendingAbsenceReminderToApprover(approver, List.of(), "http://app/absences");

            verifyNoInteractions(mailSender);
        }

        @Test
        @DisplayName("does nothing when pending list is empty")
        void doesNothingWhenPendingListIsEmpty() {
            User approver = makeApprover(1L, "Moussa", "Ndiaye", "moussa@example.com");

            emailService.sendPendingAbsenceReminderToApprover(approver, List.of(), "http://app/absences");

            verifyNoInteractions(mailSender);
        }

        @Test
        @DisplayName("sends exactly one email to the approver")
        void sendsOneEmailToApprover() {
            User approver = makeApprover(1L, "Moussa", "Ndiaye", "moussa@example.com");
            AbsenceRequest ar = defaultAbsenceRequest(new HashSet<>(Set.of(approver)));
            when(mailSender.createMimeMessage()).thenReturn(newMimeMessage());

            emailService.sendPendingAbsenceReminderToApprover(approver, List.of(ar), "http://app/absences");

            verify(mailSender, times(1)).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("email subject contains the pending count")
        void subjectContainsPendingCount() throws Exception {
            User approver = makeApprover(1L, "Moussa", "Ndiaye", "moussa@example.com");
            AbsenceRequest ar1 = defaultAbsenceRequest(new HashSet<>(Set.of(approver)));
            AbsenceRequest ar2 = buildAbsenceRequest(makeRequester(), AbsenceTypeCode.MALADE,
                    new HashSet<>(Set.of(approver)), LocalDate.of(2026, 5, 1),
                    LocalTime.of(9, 0), LocalTime.of(12, 0), "ABS-REF-002");
            MimeMessage mime = newMimeMessage();
            when(mailSender.createMimeMessage()).thenReturn(mime);

            emailService.sendPendingAbsenceReminderToApprover(approver, List.of(ar1, ar2), "http://app");

            verify(mailSender).send(messageCaptor.capture());
            assertTrue(messageCaptor.getValue().getSubject().contains("2"),
                    "Subject should mention count 2");
        }

        @Test
        @DisplayName("email body contains the reference code")
        void bodyContainsReferenceCode() throws Exception {
            User approver = makeApprover(1L, "Moussa", "Ndiaye", "moussa@example.com");
            AbsenceRequest ar = defaultAbsenceRequest(new HashSet<>(Set.of(approver)));
            MimeMessage mime = newMimeMessage();
            when(mailSender.createMimeMessage()).thenReturn(mime);

            emailService.sendPendingAbsenceReminderToApprover(approver, List.of(ar), "http://app");

            verify(mailSender).send(messageCaptor.capture());
            String body = extractHtmlBody(messageCaptor.getValue());
            assertTrue(body.contains("ABS-20260410-A1B2C3"), "Body should contain reference code");
        }

        @Test
        @DisplayName("email body contains the formatted date")
        void bodyContainsFormattedDate() throws Exception {
            User approver = makeApprover(1L, "Moussa", "Ndiaye", "moussa@example.com");
            AbsenceRequest ar = defaultAbsenceRequest(new HashSet<>(Set.of(approver)));
            MimeMessage mime = newMimeMessage();
            when(mailSender.createMimeMessage()).thenReturn(mime);

            emailService.sendPendingAbsenceReminderToApprover(approver, List.of(ar), "http://app");

            verify(mailSender).send(messageCaptor.capture());
            String body = extractHtmlBody(messageCaptor.getValue());
            assertTrue(body.contains("10/04/2026"), "Body should contain formatted date");
        }

        @Test
        @DisplayName("email body contains the approval URL")
        void bodyContainsApprovalUrl() throws Exception {
            User approver = makeApprover(1L, "Moussa", "Ndiaye", "moussa@example.com");
            AbsenceRequest ar = defaultAbsenceRequest(new HashSet<>(Set.of(approver)));
            MimeMessage mime = newMimeMessage();
            when(mailSender.createMimeMessage()).thenReturn(mime);

            emailService.sendPendingAbsenceReminderToApprover(approver, List.of(ar), "http://app/absences");

            verify(mailSender).send(messageCaptor.capture());
            String body = extractHtmlBody(messageCaptor.getValue());
            assertTrue(body.contains("http://app/absences"), "Body should contain approval URL");
        }

        @Test
        @DisplayName("does not propagate exception when mail sender fails")
        void doesNotThrowWhenMailSenderFails() {
            User approver = makeApprover(1L, "Moussa", "Ndiaye", "moussa@example.com");
            AbsenceRequest ar = defaultAbsenceRequest(new HashSet<>(Set.of(approver)));
            when(mailSender.createMimeMessage()).thenReturn(newMimeMessage());
            doThrow(new org.springframework.mail.MailSendException("SMTP error"))
                    .when(mailSender).send(any(MimeMessage.class));

            assertDoesNotThrow(() ->
                    emailService.sendPendingAbsenceReminderToApprover(approver, List.of(ar), "http://app"));
        }
    }
}

package heritage.africca.accel_connect.service;

import heritage.africca.accel_connect.dto.LeaveApplicationResponseDto;
import heritage.africca.accel_connect.dto.LeaveBalanceInfoDto;
import heritage.africca.accel_connect.dto.LeaveDecisionRequest;
import heritage.africca.accel_connect.dto.LeaveDto;
import heritage.africca.accel_connect.dto.LeaveRequestSummaryDto;
import heritage.africca.accel_connect.entity.Leave;
import heritage.africca.accel_connect.entity.LeaveBalance;
import heritage.africca.accel_connect.entity.LeaveRequest;
import heritage.africca.accel_connect.entity.User;
import heritage.africca.accel_connect.entity.enumeration.LeaveStatus;
import heritage.africca.accel_connect.entity.enumeration.LeaveTypeCode;
import heritage.africca.accel_connect.config.LeaveObserverProperties;
import heritage.africca.accel_connect.exception.ResourceNotFoundException;
import heritage.africca.accel_connect.exception.ValidationException;
import heritage.africca.accel_connect.service.Impl.NextcloudTalkService;
import org.springframework.security.access.AccessDeniedException;
import heritage.africca.accel_connect.repository.LeaveBalanceRepository;
import heritage.africca.accel_connect.repository.LeaveRequestRepository;
import heritage.africca.accel_connect.repository.TeamRepository;
import heritage.africca.accel_connect.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private LeaveRequestRepository leaveRequestRepository;
    @Mock private LeaveBalanceRepository leaveBalanceRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private LeaveBalanceService leaveBalanceService;
    @Mock private EmailService emailService;
    @Mock private NotificationService notificationService;
    @Mock private NextcloudTalkService nextcloudTalkService;
    @Mock private LeaveObserverProperties leaveObserverProperties;
    @Mock private SenegalHolidayService senegalHolidayService;

    @InjectMocks
    private LeaveService leaveService;

    private User user;
    private LeaveTypeCode leaveType;
    private LeaveDto validApplication;
    private LocalDate futureDate;

    @BeforeEach
    void setUp() {
        futureDate = LocalDate.now().plusWeeks(3);
        if (futureDate.getDayOfWeek() == DayOfWeek.SATURDAY || futureDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            futureDate = futureDate.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        }

        user = new User();
        user.setId(1L);
        user.setUsername("john.doe");
        user.setEmail("john@example.com");
        user.setKeycloakId("kc-123");

        leaveType = LeaveTypeCode.ANNUAL_LEAVE;

        lenient().when(senegalHolidayService.getHolidaysForRange(any(), any())).thenReturn(Set.of());

        validApplication = LeaveDto.builder()
                .leaveTypeCode("ANNUAL_LEAVE")
                .startDate(futureDate)
                .endDate(futureDate.plusDays(1))
                .build();
    }

    // ── applyForLeave ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("applyForLeave")
    class ApplyForLeaveTests {

        @Test
        @DisplayName("submits leave request successfully")
        void submitsLeaveSuccessfully() {
            LeaveBalance balance = new LeaveBalance(1L, user, BigDecimal.valueOf(21), BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.now());
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(leaveBalanceRepository.findByUser_Id(1L)).thenReturn(Optional.of(balance));
            when(leaveRequestRepository.findPendingByUserAndLeaveType(eq(1L), any(LeaveTypeCode.class), any(LocalDate.class)))
                    .thenReturn(List.of());
            when(leaveRequestRepository.findPendingByUser(eq(1L), any(LocalDate.class)))
                    .thenReturn(List.of());
            when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> {
                LeaveRequest lr = inv.getArgument(0);
                lr.setId(100);
                return lr;
            });

            LeaveApplicationResponseDto result = leaveService.applyForLeave(1L, validApplication);

            assertNotNull(result);
            assertEquals(100, result.getLeaveRequestId());
            assertEquals("Pending Approval", result.getStatus());
            assertTrue(result.getDaysRequested().compareTo(BigDecimal.ZERO) > 0);
            verify(userRepository).findById(1L);
            verify(leaveRequestRepository).save(any(LeaveRequest.class));
        }

        @Test
        @DisplayName("throws when user not found")
        void throwsWhenUserNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ValidationException.class, () ->
                    leaveService.applyForLeave(999L, validApplication));
            verify(leaveRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws when leave type code is unknown")
        void throwsWhenLeaveTypeNotFound() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            LeaveDto app = LeaveDto.builder()
                    .leaveTypeCode("INVALID_CODE")
                    .startDate(futureDate)
                    .endDate(futureDate)
                    .build();

            assertThrows(ValidationException.class, () ->
                    leaveService.applyForLeave(1L, app));
            verify(leaveRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("allows request with start date in the past")
        void allowsPastStartDate() {
            LocalDate pastMonday = LocalDate.now().minusWeeks(2).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LeaveBalance balance = new LeaveBalance(1L, user, BigDecimal.valueOf(21), BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.now());
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(leaveBalanceRepository.findByUser_Id(1L)).thenReturn(Optional.of(balance));
            when(leaveRequestRepository.findPendingByUserAndLeaveType(eq(1L), any(LeaveTypeCode.class), any(LocalDate.class)))
                    .thenReturn(List.of());
            when(leaveRequestRepository.findPendingByUser(eq(1L), any(LocalDate.class)))
                    .thenReturn(List.of());
            when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> {
                LeaveRequest lr = inv.getArgument(0);
                lr.setId(100);
                return lr;
            });

            LeaveDto app = LeaveDto.builder()
                    .leaveTypeCode("ANNUAL_LEAVE")
                    .startDate(pastMonday)
                    .endDate(pastMonday.plusDays(1))
                    .build();

            LeaveApplicationResponseDto result = leaveService.applyForLeave(1L, app);
            assertNotNull(result);
            assertEquals(100, result.getLeaveRequestId());
            verify(leaveRequestRepository).save(any());
        }

        @Test
        @DisplayName("throws when end date is before start date")
        void throwsWhenEndDateBeforeStartDate() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            LeaveDto app = LeaveDto.builder()
                    .leaveTypeCode("ANNUAL_LEAVE")
                    .startDate(futureDate)
                    .endDate(futureDate.minusDays(1))
                    .build();

            assertThrows(ValidationException.class, () ->
                    leaveService.applyForLeave(1L, app));
            verify(leaveRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("allows request when balance is insufficient, resulting in negative balance")
        void allowsRequestWhenInsufficientBalance() {
            LeaveBalance balance = new LeaveBalance(1L, user, BigDecimal.valueOf(21), BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.now());
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(leaveBalanceRepository.findByUser_Id(1L)).thenReturn(Optional.of(balance));
            when(leaveRequestRepository.findPendingByUserAndLeaveType(eq(1L), any(LeaveTypeCode.class), any(LocalDate.class)))
                    .thenReturn(List.of());
            when(leaveRequestRepository.findPendingByUser(eq(1L), any(LocalDate.class)))
                    .thenReturn(List.of());
            when(leaveRequestRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            LeaveDto app = LeaveDto.builder()
                    .leaveTypeCode("ANNUAL_LEAVE")
                    .startDate(futureDate)
                    .endDate(futureDate.plusDays(98))
                    .build();

            LeaveApplicationResponseDto response = leaveService.applyForLeave(1L, app);
            assertNotNull(response);
            assertTrue(response.getBalanceAfter().compareTo(BigDecimal.ZERO) < 0);
            verify(leaveRequestRepository).save(any());
        }

        @Test
        @DisplayName("half-day (< 4h) counts as zero deductible days and throws")
        void halfDayRequestResultsInHalfDay() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            LeaveDto halfDayApp = LeaveDto.builder()
                    .leaveTypeCode("ANNUAL_LEAVE")
                    .startDate(futureDate)
                    .endDate(futureDate)
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(11, 0))   // 2h < 4h → 0 deductible days
                    .build();

            assertThrows(ValidationException.class, () ->
                    leaveService.applyForLeave(1L, halfDayApp));
            verify(leaveRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("single-day (>= 4h) is deducted from balance and saved")
        void singleDayFullHoursIsDeducted() {
            LeaveBalance balance = new LeaveBalance(1L, user, BigDecimal.valueOf(10), BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.now());
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(leaveBalanceRepository.findByUser_Id(1L)).thenReturn(Optional.of(balance));
            when(leaveRequestRepository.findPendingByUserAndLeaveType(eq(1L), any(LeaveTypeCode.class), any(LocalDate.class)))
                    .thenReturn(List.of());
            when(leaveRequestRepository.findPendingByUser(eq(1L), any(LocalDate.class)))
                    .thenReturn(List.of());
            when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> {
                LeaveRequest lr = inv.getArgument(0);
                lr.setId(200);
                return lr;
            });

            LeaveDto fullPartialDay = LeaveDto.builder()
                    .leaveTypeCode("ANNUAL_LEAVE")
                    .startDate(futureDate)
                    .endDate(futureDate)
                    .startTime(LocalTime.of(8, 0))
                    .endTime(LocalTime.of(16, 0))   // 8h >= 4h → full day
                    .build();

            LeaveApplicationResponseDto result = leaveService.applyForLeave(1L, fullPartialDay);

            assertNotNull(result);
            assertEquals(200, result.getLeaveRequestId());
            assertTrue(result.getDaysRequested().compareTo(BigDecimal.ZERO) > 0);
        }

        @Test
        @DisplayName("public holiday within range is excluded from deductible days count")
        void publicHolidayIsExcludedFromDeductibleDays() {
            // Monday → Wednesday (3 calendar days), with Tuesday being a public holiday
            LocalDate monday = futureDate.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
            LocalDate tuesday = monday.plusDays(1);
            LocalDate wednesday = monday.plusDays(2);

            when(senegalHolidayService.getHolidaysForRange(any(), any())).thenReturn(Set.of(tuesday));

            LeaveBalance balance = new LeaveBalance(1L, user, BigDecimal.valueOf(21), BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.now());
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(leaveBalanceRepository.findByUser_Id(1L)).thenReturn(Optional.of(balance));
            when(leaveRequestRepository.findPendingByUserAndLeaveType(eq(1L), any(LeaveTypeCode.class), any(LocalDate.class)))
                    .thenReturn(List.of());
            when(leaveRequestRepository.findPendingByUser(eq(1L), any(LocalDate.class)))
                    .thenReturn(List.of());
            when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> {
                LeaveRequest lr = inv.getArgument(0);
                lr.setId(400);
                return lr;
            });

            LeaveDto app = LeaveDto.builder()
                    .leaveTypeCode("ANNUAL_LEAVE")
                    .startDate(monday)
                    .endDate(wednesday)
                    .build();

            LeaveApplicationResponseDto result = leaveService.applyForLeave(1L, app);

            // 3 days range, 1 public holiday → 2 deductible days
            assertEquals(0, new BigDecimal("2").compareTo(result.getDaysRequested()));
        }

        @Test
        @DisplayName("range spanning only weekend days throws ValidationException")
        void weekendOnlyRangeThrows() {
            LocalDate saturday = futureDate;
            while (saturday.getDayOfWeek() != DayOfWeek.SATURDAY) {
                saturday = saturday.plusDays(1);
            }
            LocalDate sunday = saturday.plusDays(1);

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            LeaveDto weekendApp = LeaveDto.builder()
                    .leaveTypeCode("ANNUAL_LEAVE")
                    .startDate(saturday)
                    .endDate(sunday)
                    .build();

            assertThrows(ValidationException.class, () ->
                    leaveService.applyForLeave(1L, weekendApp));
            verify(leaveRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("designated approvers are persisted on the leave request")
        void designatedApproversArePersistedOnRequest() {
            User approver = new User();
            approver.setId(5L);

            LeaveBalance balance = new LeaveBalance(1L, user, BigDecimal.valueOf(21), BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.now());
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userRepository.findById(5L)).thenReturn(Optional.of(approver));
            when(leaveBalanceRepository.findByUser_Id(1L)).thenReturn(Optional.of(balance));
            when(leaveRequestRepository.findPendingByUserAndLeaveType(eq(1L), any(LeaveTypeCode.class), any(LocalDate.class)))
                    .thenReturn(List.of());
            when(leaveRequestRepository.findPendingByUser(eq(1L), any(LocalDate.class)))
                    .thenReturn(List.of());
            when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> {
                LeaveRequest lr = inv.getArgument(0);
                lr.setId(300);
                return lr;
            });

            LeaveDto appWithApprovers = LeaveDto.builder()
                    .leaveTypeCode("ANNUAL_LEAVE")
                    .startDate(futureDate)
                    .endDate(futureDate)
                    .approverUserIds(List.of(5L))
                    .build();

            leaveService.applyForLeave(1L, appWithApprovers);

            verify(leaveRequestRepository).save(argThat(lr ->
                    lr.getApprovers() != null &&
                    lr.getApprovers().size() == 1 &&
                    lr.getApprovers().get(0).getId().equals(5L)));
        }

        @Test
        @DisplayName("zero available balance allows request, resulting in negative balance")
        void zeroBalanceAllowsRequest() {
            LeaveBalance balance = new LeaveBalance(1L, user, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.now());
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(leaveBalanceRepository.findByUser_Id(1L)).thenReturn(Optional.of(balance));
            when(leaveRequestRepository.findPendingByUserAndLeaveType(eq(1L), any(LeaveTypeCode.class), any(LocalDate.class)))
                    .thenReturn(List.of());
            when(leaveRequestRepository.findPendingByUser(eq(1L), any(LocalDate.class)))
                    .thenReturn(List.of());
            when(leaveRequestRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            LeaveApplicationResponseDto response = leaveService.applyForLeave(1L, validApplication);
            assertNotNull(response);
            assertTrue(response.getBalanceAfter().compareTo(BigDecimal.ZERO) < 0);
            verify(leaveRequestRepository).save(any());
        }
    }

    // ── getLeaveRequestById ───────────────────────────────────────────────────

    @Nested
    @DisplayName("getLeaveRequestById")
    class GetLeaveRequestByIdTests {

        @Test
        @DisplayName("returns request when found")
        void returnsWhenFound() {
            LeaveRequest leaveRequest = LeaveRequest.builder().id(1).leaves(new ArrayList<>()).build();
            when(leaveRequestRepository.findByIdWithAssociations(1)).thenReturn(Optional.of(leaveRequest));

            LeaveRequest result = leaveService.getLeaveRequestById(1);

            assertNotNull(result);
            assertEquals(1, result.getId());
            verify(leaveRequestRepository).findByIdWithAssociations(1);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when not found")
        void throwsWhenNotFound() {
            when(leaveRequestRepository.findByIdWithAssociations(999)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    leaveService.getLeaveRequestById(999));
        }
    }

    // ── approveLeaveRequest ───────────────────────────────────────────────────

    @Nested
    @DisplayName("approveLeaveRequest")
    class ApproveLeaveRequestTests {

        @Test
        @DisplayName("approves leave request and sets leaves to SCHEDULED")
        void approvesSuccessfully() {
            Leave leave1 = Leave.builder().id(1).status(LeaveStatus.PENDING_APPROVAL).lengthDays(BigDecimal.ONE).build();
            Leave leave2 = Leave.builder().id(2).status(LeaveStatus.PENDING_APPROVAL).lengthDays(BigDecimal.ONE).build();
            List<Leave> leaves = new ArrayList<>(List.of(leave1, leave2));

            LeaveRequest leaveRequest = LeaveRequest.builder()
                    .id(1).user(user).leaves(leaves).comments(new ArrayList<>()).build();

            when(leaveRequestRepository.findById(1)).thenReturn(Optional.of(leaveRequest));
            when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

            LeaveRequest result = leaveService.approveLeaveRequest(
                    1, new LeaveDecisionRequest(), 2L, Set.of("ROLE_MANAGER"));

            assertNotNull(result);
            assertEquals(LeaveStatus.SCHEDULED, leave1.getStatus());
            assertEquals(LeaveStatus.SCHEDULED, leave2.getStatus());
            verify(leaveBalanceService).deductBalanceForUser(eq(1L), eq(BigDecimal.valueOf(2)));
            verify(leaveRequestRepository).save(leaveRequest);
        }

        @Test
        @DisplayName("adds approval comment when provided")
        void addsCommentWhenProvided() {
            Leave leave = Leave.builder().id(1).status(LeaveStatus.PENDING_APPROVAL).lengthDays(BigDecimal.ONE).build();
            List<heritage.africca.accel_connect.entity.LeaveRequestComment> comments = new ArrayList<>();

            LeaveRequest leaveRequest = LeaveRequest.builder()
                    .id(1).user(user).leaves(new ArrayList<>(List.of(leave))).comments(comments).build();

            when(leaveRequestRepository.findById(1)).thenReturn(Optional.of(leaveRequest));
            when(userRepository.findById(2L)).thenReturn(Optional.of(new User()));
            when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

            LeaveDecisionRequest decision = LeaveDecisionRequest.builder()
                    .comment("Approved for team event")
                    .decidedByUserId(2L)
                    .build();

            leaveService.approveLeaveRequest(1, decision, 2L, Set.of("ROLE_HR_MANAGER"));

            assertEquals(1, leaveRequest.getComments().size());
            assertTrue(leaveRequest.getComments().get(0).getComments().startsWith("Approved:"));
        }

        @Test
        @DisplayName("throws when leave request not found")
        void throwsWhenNotFound() {
            when(leaveRequestRepository.findById(999)).thenReturn(Optional.empty());

            assertThrows(ValidationException.class, () ->
                    leaveService.approveLeaveRequest(999, new LeaveDecisionRequest(), 1L, Set.of("ROLE_MANAGER")));
            verify(leaveRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws when no pending leaves")
        void throwsWhenNoPendingLeaves() {
            Leave leave = Leave.builder().id(1).status(LeaveStatus.SCHEDULED).build();
            LeaveRequest leaveRequest = LeaveRequest.builder().id(1).leaves(List.of(leave)).build();

            when(leaveRequestRepository.findById(1)).thenReturn(Optional.of(leaveRequest));

            assertThrows(ValidationException.class, () ->
                    leaveService.approveLeaveRequest(1, new LeaveDecisionRequest(), 1L, Set.of("ROLE_MANAGER")));
            verify(leaveRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws when current user is not a designated approver")
        void throwsWhenNotDesignatedApprover() {
            User approver = new User();
            approver.setId(10L);
            Leave leave = Leave.builder().id(1).status(LeaveStatus.PENDING_APPROVAL).lengthDays(BigDecimal.ONE).build();
            LeaveRequest leaveRequest = LeaveRequest.builder()
                    .id(1).leaves(List.of(leave)).comments(new ArrayList<>()).approvers(List.of(approver)).build();

            when(leaveRequestRepository.findById(1)).thenReturn(Optional.of(leaveRequest));

            assertThrows(ValidationException.class, () ->
                    leaveService.approveLeaveRequest(1, new LeaveDecisionRequest(), 99L, Set.of("ROLE_MANAGER")));
            verify(leaveRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("HR_MANAGER can approve even when not in designated approvers")
        void allowsHrManagerToApproveRegardlessOfApprovers() {
            User approver = new User();
            approver.setId(10L);
            Leave leave = Leave.builder().id(1).status(LeaveStatus.PENDING_APPROVAL).lengthDays(BigDecimal.ONE).build();
            LeaveRequest leaveRequest = LeaveRequest.builder()
                    .id(1).user(user).leaves(List.of(leave)).comments(new ArrayList<>()).approvers(List.of(approver)).build();

            when(leaveRequestRepository.findById(1)).thenReturn(Optional.of(leaveRequest));
            when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

            LeaveRequest result = leaveService.approveLeaveRequest(
                    1, new LeaveDecisionRequest(), 99L, Set.of("ROLE_HR_MANAGER"));

            assertNotNull(result);
            assertEquals(LeaveStatus.SCHEDULED, leave.getStatus());
        }
    }

    // ── rejectLeaveRequest ────────────────────────────────────────────────────

    @Nested
    @DisplayName("rejectLeaveRequest")
    class RejectLeaveRequestTests {

        @Test
        @DisplayName("rejects leave request and sets leaves to REJECTED")
        void rejectsSuccessfully() {
            Leave leave = Leave.builder().id(1).status(LeaveStatus.PENDING_APPROVAL).lengthDays(BigDecimal.ONE).build();
            List<Leave> leaves = new ArrayList<>(List.of(leave));

            LeaveRequest leaveRequest = LeaveRequest.builder()
                    .id(1).user(user).leaves(leaves).comments(new ArrayList<>()).build();

            when(leaveRequestRepository.findById(1)).thenReturn(Optional.of(leaveRequest));
            when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

            LeaveRequest result = leaveService.rejectLeaveRequest(
                    1, new LeaveDecisionRequest(), 2L, Set.of("ROLE_MANAGER"));

            assertNotNull(result);
            assertEquals(LeaveStatus.REJECTED, leave.getStatus());
            verify(leaveRequestRepository).save(leaveRequest);
        }

        @Test
        @DisplayName("adds rejection comment when provided")
        void addsRejectionCommentWhenProvided() {
            Leave leave = Leave.builder().id(1).status(LeaveStatus.PENDING_APPROVAL).build();
            List<heritage.africca.accel_connect.entity.LeaveRequestComment> comments = new ArrayList<>();

            LeaveRequest leaveRequest = LeaveRequest.builder()
                    .id(1).user(user).leaves(new ArrayList<>(List.of(leave))).comments(comments).build();

            when(leaveRequestRepository.findById(1)).thenReturn(Optional.of(leaveRequest));
            when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

            LeaveDecisionRequest decision = LeaveDecisionRequest.builder()
                    .comment("Conflict with project deadline").build();

            leaveService.rejectLeaveRequest(1, decision, 2L, Set.of("ROLE_HR_MANAGER"));

            assertEquals(1, leaveRequest.getComments().size());
            assertTrue(leaveRequest.getComments().get(0).getComments().startsWith("Rejected:"));
        }

        @Test
        @DisplayName("throws when leave request not found")
        void throwsWhenNotFound() {
            when(leaveRequestRepository.findById(999)).thenReturn(Optional.empty());

            assertThrows(ValidationException.class, () ->
                    leaveService.rejectLeaveRequest(999, new LeaveDecisionRequest(), 1L, Set.of("ROLE_MANAGER")));
            verify(leaveRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws when no pending leaves to reject")
        void throwsWhenNoPendingLeaves() {
            Leave leave = Leave.builder().id(1).status(LeaveStatus.REJECTED).build();
            LeaveRequest leaveRequest = LeaveRequest.builder().id(1).leaves(List.of(leave)).build();

            when(leaveRequestRepository.findById(1)).thenReturn(Optional.of(leaveRequest));

            assertThrows(ValidationException.class, () ->
                    leaveService.rejectLeaveRequest(1, new LeaveDecisionRequest(), 1L, Set.of("ROLE_MANAGER")));
            verify(leaveRequestRepository, never()).save(any());
        }
    }

    // ── getUserLeaveRequests ──────────────────────────────────────────────────

    @Nested
    @DisplayName("getUserLeaveRequests")
    class GetUserLeaveRequestsTests {

        @Test
        @DisplayName("returns summaries for user's leave requests")
        void returnsSummariesForUser() {
            Leave leave = Leave.builder()
                    .id(1).date(futureDate)
                    .lengthDays(new BigDecimal("1.0000")).lengthHours(new BigDecimal("8.00"))
                    .status(LeaveStatus.PENDING_APPROVAL).durationType(0).build();

            LeaveRequest request = LeaveRequest.builder()
                    .id(1).leaveType(leaveType).dateApplied(LocalDate.now())
                    .leaves(new ArrayList<>(List.of(leave))).comments(new ArrayList<>()).build();

            when(userRepository.existsById(1L)).thenReturn(true);
            when(leaveRequestRepository.findByUserIdWithDetails(1L)).thenReturn(List.of(request));

            List<LeaveRequestSummaryDto> result = leaveService.getUserLeaveRequests(1L);

            assertEquals(1, result.size());
            LeaveRequestSummaryDto dto = result.get(0);
            assertEquals(1, dto.getId());
            assertEquals("Congé annuel", dto.getLeaveTypeName());
            assertEquals("En attente", dto.getOverallStatus());
            assertEquals(1, dto.getDays().size());
            assertEquals(futureDate, dto.getStartDate());
            assertEquals(futureDate, dto.getEndDate());
            verify(leaveRequestRepository).findByUserIdWithDetails(1L);
        }

        @Test
        @DisplayName("returns empty list when user has no requests")
        void returnsEmptyWhenNoRequests() {
            when(userRepository.existsById(1L)).thenReturn(true);
            when(leaveRequestRepository.findByUserIdWithDetails(1L)).thenReturn(List.of());

            assertTrue(leaveService.getUserLeaveRequests(1L).isEmpty());
        }

        @Test
        @DisplayName("derives status Planifié from SCHEDULED leaves")
        void derivesStatusScheduled() {
            Leave leave = Leave.builder()
                    .id(1).date(futureDate)
                    .lengthDays(new BigDecimal("1.0000")).lengthHours(new BigDecimal("8.00"))
                    .status(LeaveStatus.SCHEDULED).durationType(0).build();

            LeaveRequest request = LeaveRequest.builder()
                    .id(2).leaveType(leaveType).dateApplied(LocalDate.now())
                    .leaves(new ArrayList<>(List.of(leave))).comments(new ArrayList<>()).build();

            when(userRepository.existsById(1L)).thenReturn(true);
            when(leaveRequestRepository.findByUserIdWithDetails(1L)).thenReturn(List.of(request));

            assertEquals("Approuvé", leaveService.getUserLeaveRequests(1L).get(0).getOverallStatus());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when user not found")
        void throwsWhenUserNotFound() {
            when(userRepository.existsById(999L)).thenReturn(false);

            assertThrows(ResourceNotFoundException.class, () ->
                    leaveService.getUserLeaveRequests(999L));
            verify(leaveRequestRepository, never()).findByUserIdWithDetails(any());
        }
    }

    // ── getUserLeaveBalance ───────────────────────────────────────────────────

    @Nested
    @DisplayName("getUserLeaveBalance")
    class GetUserLeaveBalanceTests {

        @Test
        @DisplayName("returns one balance entry per leave type (all enum values)")
        void returnsBalanceForAllTypes() {
            LeaveBalance balance = new LeaveBalance(1L, user, BigDecimal.valueOf(21), BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.now());
            when(userRepository.existsById(1L)).thenReturn(true);
            when(leaveBalanceRepository.findByUser_Id(1L)).thenReturn(Optional.of(balance));
            when(leaveRequestRepository.findPendingByUserAndLeaveType(eq(1L), any(LeaveTypeCode.class), any(LocalDate.class)))
                    .thenReturn(List.of());
            when(leaveRequestRepository.findPendingByUser(eq(1L), any(LocalDate.class)))
                    .thenReturn(List.of());

            List<LeaveBalanceInfoDto> result = leaveService.getUserLeaveBalance(1L);

            assertEquals(LeaveTypeCode.values().length, result.size());
            LeaveBalanceInfoDto annualDto = result.stream()
                    .filter(d -> "ANNUAL_LEAVE".equals(d.getLeaveTypeCode()))
                    .findFirst().orElseThrow();
            assertEquals("Congé annuel", annualDto.getLeaveTypeName());
            assertTrue(annualDto.getAvailable().compareTo(BigDecimal.ZERO) > 0);
        }

        @Test
        @DisplayName("pending days reduce the available balance")
        void pendingDaysReduceAvailableBalance() {
            Leave pendingLeave = Leave.builder()
                    .id(1).date(futureDate)
                    .lengthDays(new BigDecimal("5.0000"))
                    .status(LeaveStatus.PENDING_APPROVAL).build();

            LeaveRequest pendingRequest = LeaveRequest.builder()
                    .id(1).leaveType(LeaveTypeCode.ANNUAL_LEAVE)
                    .leaves(new ArrayList<>(List.of(pendingLeave))).build();

            LeaveBalance balance = new LeaveBalance(1L, user, BigDecimal.valueOf(21), BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.now());
            when(userRepository.existsById(1L)).thenReturn(true);
            when(leaveBalanceRepository.findByUser_Id(1L)).thenReturn(Optional.of(balance));
            when(leaveRequestRepository.findPendingByUserAndLeaveType(
                    eq(1L), eq(LeaveTypeCode.ANNUAL_LEAVE), any(LocalDate.class)))
                    .thenReturn(List.of(pendingRequest));
            when(leaveRequestRepository.findPendingByUser(eq(1L), any(LocalDate.class)))
                    .thenReturn(List.of(pendingRequest));

            List<LeaveBalanceInfoDto> result = leaveService.getUserLeaveBalance(1L);

            LeaveBalanceInfoDto annualDto = result.stream()
                    .filter(d -> "ANNUAL_LEAVE".equals(d.getLeaveTypeCode()))
                    .findFirst().orElseThrow();
            assertEquals(0, new BigDecimal("5").compareTo(annualDto.getPending()));
            assertEquals(0, new BigDecimal("16").compareTo(annualDto.getAvailable()));
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when user not found")
        void throwsWhenUserNotFound() {
            when(userRepository.existsById(999L)).thenReturn(false);

            assertThrows(ResourceNotFoundException.class, () ->
                    leaveService.getUserLeaveBalance(999L));
        }
    }

    // ── getAllPendingRequests ─────────────────────────────────────────────────

    @Nested
    @DisplayName("getAllPendingRequests")
    class GetAllPendingRequestsTests {


        @Test
        @DisplayName("MANAGER sees only requests where they are designated approver")
        void managerSeesOnlyDesignatedRequests() {
            user.setFirstName("Charlotte");
            user.setLastName("Diop");

            User manager = new User();
            manager.setId(3L);

            Leave leave = Leave.builder()
                    .id(3).date(futureDate)
                    .lengthDays(BigDecimal.ONE).lengthHours(new BigDecimal("8.00"))
                    .status(LeaveStatus.PENDING_APPROVAL).durationType(0).build();

            LeaveRequest request = LeaveRequest.builder()
                    .id(12).user(user).leaveType(leaveType).dateApplied(LocalDate.now())
                    .leaves(new ArrayList<>(List.of(leave))).comments(new ArrayList<>())
                    .approvers(new ArrayList<>(List.of(manager))).build();

            when(leaveRequestRepository.findPendingLeavesByApprover(3L)).thenReturn(List.of(request));

            List<LeaveRequestSummaryDto> result = leaveService.getAllPendingRequests(3L, Set.of("ROLE_MANAGER"));

            assertEquals(1, result.size());
            assertEquals("Charlotte Diop", result.get(0).getEmployeeName());
            verify(leaveRequestRepository).findPendingLeavesByApprover(3L);
        }

        @Test
        @DisplayName("MANAGER sees empty list when not designated as approver")
        void managerSeesEmptyWhenNotApprover() {
            when(leaveRequestRepository.findPendingLeavesByApprover(5L)).thenReturn(List.of());

            List<LeaveRequestSummaryDto> result = leaveService.getAllPendingRequests(5L, Set.of("ROLE_MANAGER"));

            assertTrue(result.isEmpty());
            verify(leaveRequestRepository).findPendingLeavesByApprover(5L);
        }

        @Test
        @DisplayName("results are sorted by dateApplied descending (most recent first)")
        void sortsByDateAppliedDescending() {
            Leave leave1 = Leave.builder()
                    .id(1).date(futureDate)
                    .lengthDays(BigDecimal.ONE).lengthHours(new BigDecimal("8.00"))
                    .status(LeaveStatus.PENDING_APPROVAL).durationType(0).build();

            LeaveRequest older = LeaveRequest.builder()
                    .id(1).user(user).leaveType(leaveType).dateApplied(LocalDate.now().minusDays(5))
                    .leaves(new ArrayList<>(List.of(leave1))).comments(new ArrayList<>()).approvers(new ArrayList<>()).build();

            Leave leave2 = Leave.builder()
                    .id(2).date(futureDate)
                    .lengthDays(BigDecimal.ONE).lengthHours(new BigDecimal("8.00"))
                    .status(LeaveStatus.PENDING_APPROVAL).durationType(0).build();

            LeaveRequest newer = LeaveRequest.builder()
                    .id(2).user(user).leaveType(leaveType).dateApplied(LocalDate.now())
                    .leaves(new ArrayList<>(List.of(leave2))).comments(new ArrayList<>()).approvers(new ArrayList<>()).build();

            when(leaveRequestRepository.findPendingLeavesByApprover(1L)).thenReturn(List.of(older, newer));

            List<LeaveRequestSummaryDto> result = leaveService.getAllPendingRequests(1L, Set.of("ROLE_HR_MANAGER"));

            assertEquals(2, result.size());
            assertEquals(2, result.get(0).getId());   // newer first
            assertEquals(1, result.get(1).getId());
        }

        @Test
        @DisplayName("includes designated approvers in the DTO")
        void includesDesignatedApprovers() {
            User approver = new User();
            approver.setId(5L);
            approver.setFirstName("Mamadou");
            approver.setLastName("Sarr");

            Leave leave = Leave.builder()
                    .id(1).date(futureDate)
                    .lengthDays(BigDecimal.ONE).lengthHours(new BigDecimal("8.00"))
                    .status(LeaveStatus.PENDING_APPROVAL).durationType(0).build();

            LeaveRequest request = LeaveRequest.builder()
                    .id(12).user(user).leaveType(leaveType).dateApplied(LocalDate.now())
                    .leaves(new ArrayList<>(List.of(leave))).comments(new ArrayList<>())
                    .approvers(new ArrayList<>(List.of(approver))).build();

            when(leaveRequestRepository.findPendingLeavesByApprover(1L)).thenReturn(List.of(request));

            List<LeaveRequestSummaryDto> result = leaveService.getAllPendingRequests(1L, Set.of("ROLE_HR_MANAGER"));

            LeaveRequestSummaryDto dto = result.get(0);
            assertNotNull(dto.getApprovers());
            assertEquals(1, dto.getApprovers().size());
            assertEquals(5L, dto.getApprovers().get(0).getId());
            assertEquals("Mamadou Sarr", dto.getApprovers().get(0).getDisplayName());
        }

        @Test
        @DisplayName("populates employeeEmail in pending request summary")
        void populatesEmployeeEmailInPendingSummary() {
            user.setFirstName("Alice");
            user.setLastName("Diallo");

            Leave leave = Leave.builder()
                    .id(1).date(futureDate)
                    .lengthDays(BigDecimal.ONE).lengthHours(new BigDecimal("8.00"))
                    .status(LeaveStatus.PENDING_APPROVAL).durationType(0).build();

            LeaveRequest request = LeaveRequest.builder()
                    .id(10).user(user).leaveType(leaveType).dateApplied(LocalDate.now())
                    .leaves(new ArrayList<>(List.of(leave))).comments(new ArrayList<>()).approvers(new ArrayList<>()).build();

            when(leaveRequestRepository.findPendingLeavesByApprover(1L)).thenReturn(List.of(request));

            List<LeaveRequestSummaryDto> result = leaveService.getAllPendingRequests(1L, Set.of("ROLE_HR_MANAGER"));

            assertEquals(1, result.size());
            assertEquals("john@example.com", result.get(0).getEmployeeEmail());
        }
    }

    // ── getApprovedRequests ───────────────────────────────────────────────────

    @Nested
    @DisplayName("getApprovedRequests")
    class GetApprovedRequestsTests {

        @Test
        @DisplayName("HR_MANAGER sees all approved requests")
        void hrManagerSeesAllApprovedRequests() {
            user.setFirstName("Awa");
            user.setLastName("Ndiaye");

            Leave leave = Leave.builder()
                    .id(1).date(futureDate)
                    .lengthDays(BigDecimal.ONE).lengthHours(new BigDecimal("8.00"))
                    .status(LeaveStatus.SCHEDULED).durationType(0).build();

            LeaveRequest request = LeaveRequest.builder()
                    .id(20).user(user).leaveType(leaveType).dateApplied(LocalDate.now())
                    .leaves(new ArrayList<>(List.of(leave))).comments(new ArrayList<>()).approvers(new ArrayList<>()).build();

            when(leaveRequestRepository.findAllWithApprovedLeaves()).thenReturn(List.of(request));

            List<LeaveRequestSummaryDto> result = leaveService.getApprovedRequests(1L, Set.of("ROLE_HR_MANAGER"));

            assertEquals(1, result.size());
            assertEquals("Awa Ndiaye", result.get(0).getEmployeeName());
            verify(leaveRequestRepository).findAllWithApprovedLeaves();
            verify(leaveRequestRepository, never()).findApprovedLeavesByApprover(any());
        }

        @Test
        @DisplayName("MANAGER sees only approved requests where they are designated approver")
        void managerSeesOnlyDesignatedApprovedRequests() {
            user.setFirstName("Fatou");
            user.setLastName("Fall");

            User manager = new User();
            manager.setId(3L);

            Leave leave = Leave.builder()
                    .id(2).date(futureDate)
                    .lengthDays(BigDecimal.ONE).lengthHours(new BigDecimal("8.00"))
                    .status(LeaveStatus.SCHEDULED).durationType(0).build();

            LeaveRequest request = LeaveRequest.builder()
                    .id(21).user(user).leaveType(leaveType).dateApplied(LocalDate.now())
                    .leaves(new ArrayList<>(List.of(leave))).comments(new ArrayList<>())
                    .approvers(new ArrayList<>(List.of(manager))).build();

            when(teamRepository.findSupervisedBy(3L)).thenReturn(List.of());
            when(leaveRequestRepository.findApprovedLeavesByApprover(3L)).thenReturn(List.of(request));

            List<LeaveRequestSummaryDto> result = leaveService.getApprovedRequests(3L, Set.of("ROLE_MANAGER"));

            assertEquals(1, result.size());
            assertEquals("Fatou Fall", result.get(0).getEmployeeName());
            verify(leaveRequestRepository).findApprovedLeavesByApprover(3L);
            verify(leaveRequestRepository, never()).findAllWithApprovedLeaves();
        }

        @Test
        @DisplayName("returns empty list when manager has no approved requests")
        void returnsEmptyWhenNoApprovedRequests() {
            when(teamRepository.findSupervisedBy(5L)).thenReturn(List.of());
            when(leaveRequestRepository.findApprovedLeavesByApprover(5L)).thenReturn(List.of());

            assertTrue(leaveService.getApprovedRequests(5L, Set.of("ROLE_MANAGER")).isEmpty());
        }

        @Test
        @DisplayName("populates employeeEmail in approved request summary")
        void populatesEmployeeEmailInApprovedSummary() {
            user.setFirstName("Awa");
            user.setLastName("Ndiaye");

            Leave leave = Leave.builder()
                    .id(1).date(futureDate)
                    .lengthDays(BigDecimal.ONE).lengthHours(new BigDecimal("8.00"))
                    .status(LeaveStatus.SCHEDULED).durationType(0).build();

            LeaveRequest request = LeaveRequest.builder()
                    .id(20).user(user).leaveType(leaveType).dateApplied(LocalDate.now())
                    .leaves(new ArrayList<>(List.of(leave))).comments(new ArrayList<>()).approvers(new ArrayList<>()).build();

            when(leaveRequestRepository.findAllWithApprovedLeaves()).thenReturn(List.of(request));

            List<LeaveRequestSummaryDto> result = leaveService.getApprovedRequests(1L, Set.of("ROLE_HR_MANAGER"));

            assertEquals(1, result.size());
            assertEquals("john@example.com", result.get(0).getEmployeeEmail());
        }
    }

    // ── getRejectedRequests ───────────────────────────────────────────────────

    @Nested
    @DisplayName("getRejectedRequests")
    class GetRejectedRequestsTests {

        private LeaveRequest rejectedRequest(int id) {
            Leave leave = Leave.builder()
                    .id(id).date(futureDate)
                    .lengthDays(BigDecimal.ONE).lengthHours(new BigDecimal("8.00"))
                    .status(LeaveStatus.REJECTED).durationType(0).build();
            return LeaveRequest.builder()
                    .id(id).user(user).leaveType(leaveType).dateApplied(LocalDate.now())
                    .leaves(new ArrayList<>(List.of(leave))).comments(new ArrayList<>()).approvers(new ArrayList<>()).build();
        }

        @Test
        @DisplayName("HR_MANAGER sees all rejected requests")
        void hrManagerSeesAllRejectedRequests() {
            when(leaveRequestRepository.findAllWithRejectedLeaves()).thenReturn(List.of(rejectedRequest(30)));

            List<LeaveRequestSummaryDto> result = leaveService.getRejectedRequests(1L, Set.of("ROLE_HR_MANAGER"));

            assertEquals(1, result.size());
            assertEquals("Rejeté", result.get(0).getOverallStatus());
            verify(leaveRequestRepository, never()).findRejectedLeavesByApprover(any());
        }

        @Test
        @DisplayName("MANAGER without supervised teams sees only designated rejected requests")
        void managerSeesOnlyDesignatedRejectedRequests() {
            when(teamRepository.findSupervisedBy(3L)).thenReturn(List.of());
            when(leaveRequestRepository.findRejectedLeavesByApprover(3L)).thenReturn(List.of(rejectedRequest(31)));

            List<LeaveRequestSummaryDto> result = leaveService.getRejectedRequests(3L, Set.of("ROLE_MANAGER"));

            assertEquals(1, result.size());
            verify(leaveRequestRepository, never()).findRejectedByTeams(any());
            verify(leaveRequestRepository, never()).findAllWithRejectedLeaves();
        }

        @Test
        @DisplayName("MANAGER supervising teams also sees team rejected requests, without duplicates")
        void managerSeesSupervisedTeamRejectedRequests() {
            heritage.africca.accel_connect.entity.Team team = new heritage.africca.accel_connect.entity.Team();
            team.setId(7L);
            LeaveRequest designated = rejectedRequest(32);
            LeaveRequest teamOnly = rejectedRequest(33);

            when(teamRepository.findSupervisedBy(3L)).thenReturn(List.of(team));
            when(leaveRequestRepository.findRejectedLeavesByApprover(3L)).thenReturn(List.of(designated));
            when(leaveRequestRepository.findRejectedByTeams(List.of(7L))).thenReturn(List.of(designated, teamOnly));

            List<LeaveRequestSummaryDto> result = leaveService.getRejectedRequests(3L, Set.of("ROLE_MANAGER"));

            assertEquals(2, result.size());
        }
    }

    // ── cancelRejectedLeaveRequest ────────────────────────────────────────────

    @Nested
    @DisplayName("cancelRejectedLeaveRequest")
    class CancelRejectedLeaveRequestTests {

        @Test
        @DisplayName("puts rejected leaves back to PENDING_APPROVAL without touching the balance")
        void revertsToPending() {
            User manager = new User();
            manager.setId(3L);
            Leave leave = Leave.builder().id(1).status(LeaveStatus.REJECTED).lengthDays(BigDecimal.ONE).build();
            LeaveRequest leaveRequest = LeaveRequest.builder()
                    .id(1).user(user).leaves(new ArrayList<>(List.of(leave))).comments(new ArrayList<>())
                    .approvers(new ArrayList<>(List.of(manager))).decidedBy(manager).build();

            when(leaveRequestRepository.findById(1)).thenReturn(Optional.of(leaveRequest));
            when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

            leaveService.cancelRejectedLeaveRequest(1, new LeaveDecisionRequest(), 3L, Set.of("ROLE_MANAGER"));

            assertEquals(LeaveStatus.PENDING_APPROVAL, leave.getStatus());
            assertNull(leaveRequest.getDecidedBy());
            verify(leaveRequestRepository).save(leaveRequest);
            verify(notificationService).createForUser(eq(1L), anyString(), anyString(), anyString());
            verifyNoInteractions(leaveBalanceService);
        }

        @Test
        @DisplayName("adds a 'Rejection cancelled' comment when provided")
        void addsCommentWhenProvided() {
            Leave leave = Leave.builder().id(1).status(LeaveStatus.REJECTED).build();
            LeaveRequest leaveRequest = LeaveRequest.builder()
                    .id(1).user(user).leaves(new ArrayList<>(List.of(leave))).comments(new ArrayList<>()).build();

            when(leaveRequestRepository.findById(1)).thenReturn(Optional.of(leaveRequest));
            when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

            LeaveDecisionRequest decision = LeaveDecisionRequest.builder().comment("Rejet par erreur").build();
            leaveService.cancelRejectedLeaveRequest(1, decision, 2L, Set.of("ROLE_HR_MANAGER"));

            assertEquals(1, leaveRequest.getComments().size());
            assertTrue(leaveRequest.getComments().get(0).getComments().startsWith("Rejection cancelled:"));
        }

        @Test
        @DisplayName("throws when request has no rejected leaves")
        void throwsWhenNotRejected() {
            Leave leave = Leave.builder().id(1).status(LeaveStatus.SCHEDULED).build();
            LeaveRequest leaveRequest = LeaveRequest.builder().id(1).user(user).leaves(List.of(leave)).build();

            when(leaveRequestRepository.findById(1)).thenReturn(Optional.of(leaveRequest));

            assertThrows(ValidationException.class, () ->
                    leaveService.cancelRejectedLeaveRequest(1, new LeaveDecisionRequest(), 2L, Set.of("ROLE_HR_MANAGER")));
            verify(leaveRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws when manager is not a designated approver")
        void throwsWhenNotDesignated() {
            User otherManager = new User();
            otherManager.setId(4L);
            Leave leave = Leave.builder().id(1).status(LeaveStatus.REJECTED).build();
            LeaveRequest leaveRequest = LeaveRequest.builder()
                    .id(1).user(user).leaves(List.of(leave)).approvers(new ArrayList<>(List.of(otherManager))).build();

            when(leaveRequestRepository.findById(1)).thenReturn(Optional.of(leaveRequest));

            assertThrows(ValidationException.class, () ->
                    leaveService.cancelRejectedLeaveRequest(1, new LeaveDecisionRequest(), 99L, Set.of("ROLE_MANAGER")));
            verify(leaveRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws when leave request not found")
        void throwsWhenNotFound() {
            when(leaveRequestRepository.findById(999)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    leaveService.cancelRejectedLeaveRequest(999, new LeaveDecisionRequest(), 1L, Set.of("ROLE_HR_MANAGER")));
        }
    }

    // ── validateLeaveRequestViewAccess ────────────────────────────────────────

    @Nested
    @DisplayName("validateLeaveRequestViewAccess")
    class ValidateLeaveRequestViewAccessTests {

        @Test
        @DisplayName("employee can view their own request")
        void employeeCanViewOwnRequest() {
            LeaveRequest request = LeaveRequest.builder().id(1).user(user).build();
            assertDoesNotThrow(() ->
                    leaveService.validateLeaveRequestViewAccess(request, user.getId(), Set.of("ROLE_USER")));
        }

        @Test
        @DisplayName("HR_MANAGER can view any request")
        void hrManagerCanViewAnyRequest() {
            User otherUser = new User();
            otherUser.setId(999L);
            LeaveRequest request = LeaveRequest.builder().id(1).user(otherUser).build();
            assertDoesNotThrow(() ->
                    leaveService.validateLeaveRequestViewAccess(request, 1L, Set.of("ROLE_HR_MANAGER")));
        }

        @Test
        @DisplayName("designated approver can view request")
        void designatedApproverCanViewRequest() {
            User approver = new User();
            approver.setId(5L);
            LeaveRequest request = LeaveRequest.builder()
                    .id(1).user(user).approvers(new ArrayList<>(List.of(approver))).build();
            assertDoesNotThrow(() ->
                    leaveService.validateLeaveRequestViewAccess(request, 5L, Set.of("ROLE_MANAGER")));
        }

        @Test
        @DisplayName("non-approver manager cannot view request")
        void nonApproverCannotViewRequest() {
            User otherApprover = new User();
            otherApprover.setId(99L);
            LeaveRequest request = LeaveRequest.builder()
                    .id(1).user(user).approvers(new ArrayList<>(List.of(otherApprover))).build();
            assertThrows(AccessDeniedException.class, () ->
                    leaveService.validateLeaveRequestViewAccess(request, 5L, Set.of("ROLE_MANAGER")));
        }

        @Test
        @DisplayName("throws ResourceNotFoundException for null request")
        void throwsExceptionForNullRequest() {
            assertThrows(ResourceNotFoundException.class, () ->
                    leaveService.validateLeaveRequestViewAccess(null, 1L, Set.of("ROLE_USER")));
        }
    }

    // ── deleteLeaveRequest ────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteLeaveRequest")
    class DeleteLeaveRequestTests {

        @Test
        @DisplayName("deletes pending request when user is the owner")
        void deletesSuccessfully() {
            Leave leave = Leave.builder().id(1).status(LeaveStatus.PENDING_APPROVAL).build();
            LeaveRequest leaveRequest = LeaveRequest.builder()
                    .id(1).user(user).leaves(new ArrayList<>(List.of(leave))).build();

            when(leaveRequestRepository.findByIdWithAssociations(1)).thenReturn(Optional.of(leaveRequest));

            leaveService.deleteLeaveRequest(1, 1L);

            verify(leaveRequestRepository).delete(leaveRequest);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when request not found")
        void throwsWhenRequestNotFound() {
            when(leaveRequestRepository.findByIdWithAssociations(999)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    leaveService.deleteLeaveRequest(999, 1L));
            verify(leaveRequestRepository, never()).delete(any());
        }

        @Test
        @DisplayName("throws ValidationException when user is not the owner")
        void throwsWhenNotOwner() {
            User otherUser = new User();
            otherUser.setId(99L);

            Leave leave = Leave.builder().id(1).status(LeaveStatus.PENDING_APPROVAL).build();
            LeaveRequest leaveRequest = LeaveRequest.builder()
                    .id(1).user(otherUser).leaves(new ArrayList<>(List.of(leave))).build();

            when(leaveRequestRepository.findByIdWithAssociations(1)).thenReturn(Optional.of(leaveRequest));

            assertThrows(ValidationException.class, () ->
                    leaveService.deleteLeaveRequest(1, 1L));
            verify(leaveRequestRepository, never()).delete(any());
        }

        @Test
        @DisplayName("throws ValidationException when no pending leaves (already scheduled)")
        void throwsWhenNoPendingLeaves() {
            Leave leave = Leave.builder().id(1).status(LeaveStatus.SCHEDULED).build();
            LeaveRequest leaveRequest = LeaveRequest.builder()
                    .id(1).user(user).leaves(new ArrayList<>(List.of(leave))).build();

            when(leaveRequestRepository.findByIdWithAssociations(1)).thenReturn(Optional.of(leaveRequest));

            assertThrows(ValidationException.class, () ->
                    leaveService.deleteLeaveRequest(1, 1L));
            verify(leaveRequestRepository, never()).delete(any());
        }
    }

    // ── cancelLeaveRequest ────────────────────────────────────────────────────

    @Nested
    @DisplayName("cancelLeaveRequest")
    class CancelLeaveRequestTests {

        @Test
        @DisplayName("cancels pending leaves and saves")
        void cancelsSuccessfully() {
            Leave leave = Leave.builder().id(1).status(LeaveStatus.PENDING_APPROVAL).build();
            LeaveRequest leaveRequest = LeaveRequest.builder()
                    .id(1).user(user).leaves(new ArrayList<>(List.of(leave))).build();

            when(leaveRequestRepository.findById(1)).thenReturn(Optional.of(leaveRequest));
            when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

            leaveService.cancelLeaveRequest(1, 1L);

            assertEquals(LeaveStatus.CANCELLED, leave.getStatus());
            verify(leaveRequestRepository).save(leaveRequest);
        }

        @Test
        @DisplayName("only cancels PENDING_APPROVAL leaves, leaves SCHEDULED untouched")
        void onlyCancelsPendingLeaves() {
            Leave pendingLeave = Leave.builder().id(1).status(LeaveStatus.PENDING_APPROVAL).build();
            Leave scheduledLeave = Leave.builder().id(2).status(LeaveStatus.SCHEDULED).build();

            LeaveRequest leaveRequest = LeaveRequest.builder()
                    .id(1).user(user).leaves(new ArrayList<>(List.of(pendingLeave, scheduledLeave))).build();

            when(leaveRequestRepository.findById(1)).thenReturn(Optional.of(leaveRequest));
            when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

            leaveService.cancelLeaveRequest(1, 1L);

            assertEquals(LeaveStatus.CANCELLED, pendingLeave.getStatus());
            assertEquals(LeaveStatus.SCHEDULED, scheduledLeave.getStatus());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when request not found")
        void throwsWhenRequestNotFound() {
            when(leaveRequestRepository.findById(999)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () ->
                    leaveService.cancelLeaveRequest(999, 1L));
            verify(leaveRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws ValidationException when user is not the owner")
        void throwsWhenNotOwner() {
            User otherUser = new User();
            otherUser.setId(99L);

            LeaveRequest leaveRequest = LeaveRequest.builder()
                    .id(1).user(otherUser).leaves(new ArrayList<>()).build();

            when(leaveRequestRepository.findById(1)).thenReturn(Optional.of(leaveRequest));

            assertThrows(ValidationException.class, () ->
                    leaveService.cancelLeaveRequest(1, 1L));
            verify(leaveRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws ValidationException when no pending leaves to cancel")
        void throwsWhenNoPendingLeaves() {
            Leave leave = Leave.builder().id(1).status(LeaveStatus.SCHEDULED).build();
            LeaveRequest leaveRequest = LeaveRequest.builder()
                    .id(1).user(user).leaves(new ArrayList<>(List.of(leave))).build();

            when(leaveRequestRepository.findById(1)).thenReturn(Optional.of(leaveRequest));

            assertThrows(ValidationException.class, () ->
                    leaveService.cancelLeaveRequest(1, 1L));
            verify(leaveRequestRepository, never()).save(any());
        }
    }
}

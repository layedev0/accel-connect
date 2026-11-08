package heritage.africca.accel_connect.service;

import heritage.africca.accel_connect.dto.AbsenceDecisionRequest;
import heritage.africca.accel_connect.dto.AbsenceRequestDto;
import heritage.africca.accel_connect.dto.AbsenceSummaryDto;
import heritage.africca.accel_connect.entity.AbsenceRequest;
import heritage.africca.accel_connect.entity.User;
import heritage.africca.accel_connect.entity.enumeration.AbsenceStatus;
import heritage.africca.accel_connect.entity.enumeration.AbsenceTypeCode;
import heritage.africca.accel_connect.config.LeaveObserverProperties;
import heritage.africca.accel_connect.exception.ResourceNotFoundException;
import heritage.africca.accel_connect.exception.ValidationException;
import heritage.africca.accel_connect.repository.AbsenceRequestRepository;
import heritage.africca.accel_connect.repository.TeamRepository;
import heritage.africca.accel_connect.repository.UserRepository;
import heritage.africca.accel_connect.service.Impl.NextcloudTalkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbsenceServiceTest {

    @Mock private AbsenceRequestRepository absenceRequestRepository;
    @Mock private UserRepository userRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private EmailService emailService;
    @Mock private NotificationService notificationService;
    @Mock private NextcloudTalkService nextcloudTalkService;
    @Mock private LeaveObserverProperties leaveObserverProperties;

    @InjectMocks
    private AbsenceService absenceService;

    private User employee;
    private User manager;
    private AbsenceRequestDto validDto;

    @BeforeEach
    void setUp() {
        employee = new User();
        employee.setId(1L);
        employee.setFirstName("Jean");
        employee.setLastName("Dupont");

        manager = new User();
        manager.setId(2L);
        manager.setFirstName("Marie");
        manager.setLastName("Martin");

        validDto = AbsenceRequestDto.builder()
                .absenceType("Malade")
                .date(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(12, 0))
                .reason("Rendez-vous médical")
                .approverUserIds(List.of(2L))
                .build();
    }

    @Nested
    @DisplayName("submitAbsence")
    class SubmitAbsenceTests {

        @Test
        @DisplayName("creates absence request and returns summary")
        void createsSuccessfully() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
            when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
            when(absenceRequestRepository.save(any(AbsenceRequest.class))).thenAnswer(inv -> {
                AbsenceRequest r = inv.getArgument(0);
                r.setId(100);
                return r;
            });

            AbsenceSummaryDto result = absenceService.submitAbsence(1L, validDto);

            assertNotNull(result);
            assertEquals(100, result.getId());
            assertEquals("Jean Dupont", result.getEmployeeName());
            assertEquals("Malade", result.getAbsenceTypeName());
            assertEquals("En attente", result.getStatus());
            assertNotNull(result.getReferenceCode());
            assertTrue(result.getReferenceCode().startsWith("ABS-"));
            verify(absenceRequestRepository).save(any(AbsenceRequest.class));
            verify(emailService).notifyApproversOfNewAbsenceRequest(any(AbsenceRequest.class));
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when employee not found")
        void throwsWhenEmployeeNotFound() {
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> absenceService.submitAbsence(1L, validDto));
            verify(absenceRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws IllegalArgumentException when absence type is unknown")
        void throwsWhenAbsenceTypeUnknown() {
            AbsenceRequestDto badDto = AbsenceRequestDto.builder()
                    .absenceType("UnknownType")
                    .date(LocalDate.now().plusDays(1))
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(12, 0))
                    .approverUserIds(List.of())
                    .build();
            when(userRepository.findById(1L)).thenReturn(Optional.of(employee));

            assertThrows(IllegalArgumentException.class,
                    () -> absenceService.submitAbsence(1L, badDto));
            verify(absenceRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws ValidationException when end time is before start time")
        void throwsWhenEndBeforeStart() {
            AbsenceRequestDto badDto = AbsenceRequestDto.builder()
                    .absenceType("Malade")
                    .date(LocalDate.now())
                    .startTime(LocalTime.of(14, 0))
                    .endTime(LocalTime.of(10, 0))
                    .approverUserIds(List.of(2L))
                    .build();
            when(userRepository.findById(1L)).thenReturn(Optional.of(employee));

            assertThrows(ValidationException.class,
                    () -> absenceService.submitAbsence(1L, badDto));
            verify(absenceRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws ValidationException when date is in the past")
        void throwsWhenDateInPast() {
            AbsenceRequestDto badDto = AbsenceRequestDto.builder()
                    .absenceType("Malade")
                    .date(LocalDate.now().minusDays(1))
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(12, 0))
                    .approverUserIds(List.of(2L))
                    .build();
            when(userRepository.findById(1L)).thenReturn(Optional.of(employee));

            assertThrows(ValidationException.class,
                    () -> absenceService.submitAbsence(1L, badDto));
            verify(absenceRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("reference code follows ABS-YYYYMMDD-XXXXXX format")
        void referenceCodeFormat() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
            when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
            when(absenceRequestRepository.save(any(AbsenceRequest.class))).thenAnswer(inv -> inv.getArgument(0));

            AbsenceSummaryDto result = absenceService.submitAbsence(1L, validDto);

            assertTrue(result.getReferenceCode().matches("ABS-\\d{8}-[A-Z0-9]{6}"));
        }
    }

    @Nested
    @DisplayName("getUserAbsences")
    class GetUserAbsencesTests {

        @Test
        @DisplayName("returns mapped list for the given user")
        void returnsMappedList() {
            AbsenceRequest req = buildPersistedRequest(AbsenceStatus.PENDING_APPROVAL);
            when(absenceRequestRepository.findByUserIdWithDetails(1L)).thenReturn(List.of(req));

            List<AbsenceSummaryDto> result = absenceService.getUserAbsences(1L);

            assertEquals(1, result.size());
            assertEquals("Jean Dupont", result.get(0).getEmployeeName());
            verify(absenceRequestRepository).findByUserIdWithDetails(1L);
        }

        @Test
        @DisplayName("returns empty list when user has no absences")
        void returnsEmptyList() {
            when(absenceRequestRepository.findByUserIdWithDetails(1L)).thenReturn(List.of());

            assertTrue(absenceService.getUserAbsences(1L).isEmpty());
        }
    }

    @Nested
    @DisplayName("getAbsenceById")
    class GetAbsenceByIdTests {

        @Test
        @DisplayName("returns summary when found")
        void returnsWhenFound() {
            AbsenceRequest req = buildPersistedRequest(AbsenceStatus.PENDING_APPROVAL);
            when(absenceRequestRepository.findByIdWithAssociations(100)).thenReturn(Optional.of(req));

            AbsenceSummaryDto result = absenceService.getAbsenceById(100);

            assertNotNull(result);
            assertEquals("En attente", result.getStatus());
            assertEquals("Malade", result.getAbsenceTypeName());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when id does not exist")
        void throwsWhenNotFound() {
            when(absenceRequestRepository.findByIdWithAssociations(999)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> absenceService.getAbsenceById(999));
        }
    }

    @Nested
    @DisplayName("getPendingAbsences")
    class GetPendingAbsencesTests {

        @Test
        @DisplayName("HR_MANAGER sees all pending requests")
        void hrManagerSeesAll() {
            AbsenceRequest req = buildPersistedRequest(AbsenceStatus.PENDING_APPROVAL);
            when(absenceRequestRepository.findByStatus(AbsenceStatus.PENDING_APPROVAL))
                    .thenReturn(List.of(req));

            List<AbsenceSummaryDto> result = absenceService.getPendingAbsences(
                    2L, Set.of("ROLE_HR_MANAGER"));

            assertEquals(1, result.size());
            verify(absenceRequestRepository).findByStatus(AbsenceStatus.PENDING_APPROVAL);
            verify(absenceRequestRepository, never()).findPendingByApprover(any(), any());
        }

        @Test
        @DisplayName("MANAGER sees only requests assigned to them")
        void managerSeesOwnApproverRequests() {
            AbsenceRequest req = buildPersistedRequest(AbsenceStatus.PENDING_APPROVAL);
            when(absenceRequestRepository.findPendingByApprover(AbsenceStatus.PENDING_APPROVAL, 2L))
                    .thenReturn(List.of(req));

            List<AbsenceSummaryDto> result = absenceService.getPendingAbsences(
                    2L, Set.of("ROLE_MANAGER"));

            assertEquals(1, result.size());
            verify(absenceRequestRepository).findPendingByApprover(AbsenceStatus.PENDING_APPROVAL, 2L);
            verify(absenceRequestRepository, never()).findByStatus(any());
        }
    }

    @Nested
    @DisplayName("getApprovedAbsences")
    class GetApprovedAbsencesTests {

        @Test
        @DisplayName("HR_MANAGER sees all approved requests")
        void hrManagerSeesAll() {
            AbsenceRequest req = buildPersistedRequest(AbsenceStatus.APPROVED);
            when(absenceRequestRepository.findByStatus(AbsenceStatus.APPROVED))
                    .thenReturn(List.of(req));

            List<AbsenceSummaryDto> result = absenceService.getApprovedAbsences(
                    2L, Set.of("ROLE_HR_MANAGER"));

            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("MANAGER sees only approved requests where they are an approver")
        void managerSeesOnlyAssigned() {
            AbsenceRequest req = buildPersistedRequest(AbsenceStatus.APPROVED);
            req.setApprovers(Set.of(manager));

            AbsenceRequest other = buildPersistedRequest(AbsenceStatus.APPROVED);
            other.setApprovers(new HashSet<>());

            when(teamRepository.findSupervisedBy(2L)).thenReturn(List.of());
            when(absenceRequestRepository.findByStatus(AbsenceStatus.APPROVED))
                    .thenReturn(List.of(req, other));

            List<AbsenceSummaryDto> result = absenceService.getApprovedAbsences(
                    2L, Set.of("ROLE_MANAGER"));

            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("populates employeeEmail in approved absence summary")
        void populatesEmployeeEmail() {
            employee.setEmail("jean.dupont@example.com");
            AbsenceRequest req = buildPersistedRequest(AbsenceStatus.APPROVED);

            when(absenceRequestRepository.findByStatus(AbsenceStatus.APPROVED))
                    .thenReturn(List.of(req));

            List<AbsenceSummaryDto> result = absenceService.getApprovedAbsences(
                    2L, Set.of("ROLE_HR_MANAGER"));

            assertEquals(1, result.size());
            assertEquals("jean.dupont@example.com", result.get(0).getEmployeeEmail());
        }
    }

    @Nested
    @DisplayName("getRejectedAbsences")
    class GetRejectedAbsencesTests {

        @Test
        @DisplayName("HR_MANAGER sees all rejected requests")
        void hrManagerSeesAll() {
            AbsenceRequest req = buildPersistedRequest(AbsenceStatus.REJECTED);
            when(absenceRequestRepository.findByStatus(AbsenceStatus.REJECTED))
                    .thenReturn(List.of(req));

            List<AbsenceSummaryDto> result = absenceService.getRejectedAbsences(
                    2L, Set.of("ROLE_HR_MANAGER"));

            assertEquals(1, result.size());
            verify(absenceRequestRepository).findByStatus(AbsenceStatus.REJECTED);
        }

        @Test
        @DisplayName("MANAGER sees only rejected requests where they are an approver")
        void managerSeesOnlyAssigned() {
            AbsenceRequest req = buildPersistedRequest(AbsenceStatus.REJECTED);
            req.setApprovers(Set.of(manager));

            AbsenceRequest other = buildPersistedRequest(AbsenceStatus.REJECTED);
            other.setApprovers(new HashSet<>());

            when(teamRepository.findSupervisedBy(2L)).thenReturn(List.of());
            when(absenceRequestRepository.findByStatus(AbsenceStatus.REJECTED))
                    .thenReturn(List.of(req, other));

            List<AbsenceSummaryDto> result = absenceService.getRejectedAbsences(
                    2L, Set.of("ROLE_MANAGER"));

            assertEquals(1, result.size());
        }
    }

    @Nested
    @DisplayName("updateAbsence")
    class UpdateAbsenceTests {

        @Test
        @DisplayName("updates successfully when pending and owned by user")
        void updatesSuccessfully() {
            AbsenceRequest req = buildPersistedRequest(AbsenceStatus.PENDING_APPROVAL);
            when(absenceRequestRepository.findByIdWithAssociations(100)).thenReturn(Optional.of(req));
            when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
            when(absenceRequestRepository.save(any(AbsenceRequest.class))).thenAnswer(inv -> inv.getArgument(0));

            AbsenceSummaryDto result = absenceService.updateAbsence(100, 1L, validDto);

            assertNotNull(result);
            verify(absenceRequestRepository).save(any(AbsenceRequest.class));
        }

        @Test
        @DisplayName("throws ValidationException when user is not the owner")
        void throwsWhenNotOwner() {
            AbsenceRequest req = buildPersistedRequest(AbsenceStatus.PENDING_APPROVAL);
            when(absenceRequestRepository.findByIdWithAssociations(100)).thenReturn(Optional.of(req));

            assertThrows(ValidationException.class,
                    () -> absenceService.updateAbsence(100, 99L, validDto));
            verify(absenceRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws ValidationException when request is already approved")
        void throwsWhenNotPending() {
            AbsenceRequest req = buildPersistedRequest(AbsenceStatus.APPROVED);
            when(absenceRequestRepository.findByIdWithAssociations(100)).thenReturn(Optional.of(req));

            assertThrows(ValidationException.class,
                    () -> absenceService.updateAbsence(100, 1L, validDto));
            verify(absenceRequestRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("approveAbsence")
    class ApproveAbsenceTests {

        @Test
        @DisplayName("HR_MANAGER can approve any pending request")
        void hrManagerApprovesSuccessfully() {
            AbsenceRequest req = buildPersistedRequest(AbsenceStatus.PENDING_APPROVAL);
            when(absenceRequestRepository.findByIdWithAssociations(100)).thenReturn(Optional.of(req));
            when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
            when(absenceRequestRepository.save(any(AbsenceRequest.class))).thenAnswer(inv -> inv.getArgument(0));

            AbsenceSummaryDto result = absenceService.approveAbsence(
                    100, null, 2L, Set.of("ROLE_HR_MANAGER"));

            assertEquals("Approuvé", result.getStatus());
        }

        @Test
        @DisplayName("designated approver can approve the request")
        void approverCanApprove() {
            AbsenceRequest req = buildPersistedRequest(AbsenceStatus.PENDING_APPROVAL);
            req.setApprovers(Set.of(manager));
            when(absenceRequestRepository.findByIdWithAssociations(100)).thenReturn(Optional.of(req));
            when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
            when(absenceRequestRepository.save(any(AbsenceRequest.class))).thenAnswer(inv -> inv.getArgument(0));

            AbsenceSummaryDto result = absenceService.approveAbsence(
                    100, null, 2L, Set.of("ROLE_MANAGER"));

            assertEquals("Approuvé", result.getStatus());
        }

        @Test
        @DisplayName("throws ValidationException when request is already approved")
        void throwsWhenAlreadyApproved() {
            AbsenceRequest req = buildPersistedRequest(AbsenceStatus.APPROVED);
            when(absenceRequestRepository.findByIdWithAssociations(100)).thenReturn(Optional.of(req));

            assertThrows(ValidationException.class,
                    () -> absenceService.approveAbsence(100, null, 2L, Set.of("ROLE_HR_MANAGER")));
        }

        @Test
        @DisplayName("throws ValidationException when non-approver manager tries to approve")
        void throwsWhenNotAuthorized() {
            AbsenceRequest req = buildPersistedRequest(AbsenceStatus.PENDING_APPROVAL);
            req.setApprovers(new HashSet<>());
            when(absenceRequestRepository.findByIdWithAssociations(100)).thenReturn(Optional.of(req));

            assertThrows(ValidationException.class,
                    () -> absenceService.approveAbsence(100, null, 99L, Set.of("ROLE_MANAGER")));
        }
    }

    @Nested
    @DisplayName("rejectAbsence")
    class RejectAbsenceTests {

        @Test
        @DisplayName("HR_MANAGER can reject any pending request")
        void hrManagerRejectsSuccessfully() {
            AbsenceRequest req = buildPersistedRequest(AbsenceStatus.PENDING_APPROVAL);
            AbsenceDecisionRequest decision = AbsenceDecisionRequest.builder()
                    .comment("Motif insuffisant").decidedByUserId(2L).build();
            when(absenceRequestRepository.findByIdWithAssociations(100)).thenReturn(Optional.of(req));
            when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
            when(absenceRequestRepository.save(any(AbsenceRequest.class))).thenAnswer(inv -> inv.getArgument(0));

            AbsenceSummaryDto result = absenceService.rejectAbsence(
                    100, decision, 2L, Set.of("ROLE_HR_MANAGER"));

            assertEquals("Rejeté", result.getStatus());
        }

        @Test
        @DisplayName("throws ValidationException when request is already rejected")
        void throwsWhenAlreadyRejected() {
            AbsenceRequest req = buildPersistedRequest(AbsenceStatus.REJECTED);
            when(absenceRequestRepository.findByIdWithAssociations(100)).thenReturn(Optional.of(req));

            assertThrows(ValidationException.class,
                    () -> absenceService.rejectAbsence(100, null, 2L, Set.of("ROLE_HR_MANAGER")));
        }
    }

    @Nested
    @DisplayName("cancelApprovedAbsence")
    class CancelApprovedAbsenceTests {

        @Test
        @DisplayName("HR_MANAGER can cancel any approved request")
        void hrManagerCancelsSuccessfully() {
            AbsenceRequest req = buildPersistedRequest(AbsenceStatus.APPROVED);
            AbsenceDecisionRequest decision = AbsenceDecisionRequest.builder()
                    .comment("Changement de planning").decidedByUserId(2L).build();
            when(absenceRequestRepository.findByIdWithAssociations(100)).thenReturn(Optional.of(req));
            when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
            when(absenceRequestRepository.save(any(AbsenceRequest.class))).thenAnswer(inv -> inv.getArgument(0));

            AbsenceSummaryDto result = absenceService.cancelApprovedAbsence(
                    100, decision, 2L, Set.of("ROLE_HR_MANAGER"));

            assertEquals("Annulé", result.getStatus());
            assertEquals(AbsenceStatus.CANCELLED, req.getStatus());
            verify(notificationService).createForUser(eq(1L), anyString(), anyString(), anyString());
            verify(emailService).notifyEmployeeOfAbsenceCancellation(req);
        }

        @Test
        @DisplayName("designated approver can cancel the request")
        void approverCanCancel() {
            AbsenceRequest req = buildPersistedRequest(AbsenceStatus.APPROVED);
            req.setApprovers(Set.of(manager));
            when(absenceRequestRepository.findByIdWithAssociations(100)).thenReturn(Optional.of(req));
            when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
            when(absenceRequestRepository.save(any(AbsenceRequest.class))).thenAnswer(inv -> inv.getArgument(0));

            AbsenceSummaryDto result = absenceService.cancelApprovedAbsence(
                    100, null, 2L, Set.of("ROLE_MANAGER"));

            assertEquals("Annulé", result.getStatus());
        }

        @Test
        @DisplayName("throws ValidationException when request is not approved")
        void throwsWhenNotApproved() {
            AbsenceRequest req = buildPersistedRequest(AbsenceStatus.PENDING_APPROVAL);
            when(absenceRequestRepository.findByIdWithAssociations(100)).thenReturn(Optional.of(req));

            assertThrows(ValidationException.class,
                    () -> absenceService.cancelApprovedAbsence(100, null, 2L, Set.of("ROLE_HR_MANAGER")));
            verify(absenceRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws ValidationException when non-approver manager tries to cancel")
        void throwsWhenNotAuthorized() {
            AbsenceRequest req = buildPersistedRequest(AbsenceStatus.APPROVED);
            req.setApprovers(new HashSet<>());
            when(absenceRequestRepository.findByIdWithAssociations(100)).thenReturn(Optional.of(req));

            assertThrows(ValidationException.class,
                    () -> absenceService.cancelApprovedAbsence(100, null, 99L, Set.of("ROLE_MANAGER")));
            verify(absenceRequestRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("cancelRejectedAbsence")
    class CancelRejectedAbsenceTests {

        @Test
        @DisplayName("designated approver puts the rejected request back to pending")
        void approverRevertsToPending() {
            AbsenceRequest req = buildPersistedRequest(AbsenceStatus.REJECTED);
            req.setApprovers(Set.of(manager));
            req.setDecidedBy(manager);
            when(absenceRequestRepository.findByIdWithAssociations(100)).thenReturn(Optional.of(req));
            when(absenceRequestRepository.save(any(AbsenceRequest.class))).thenAnswer(inv -> inv.getArgument(0));

            AbsenceSummaryDto result = absenceService.cancelRejectedAbsence(
                    100, null, 2L, Set.of("ROLE_MANAGER"));

            assertEquals("En attente", result.getStatus());
            assertEquals(AbsenceStatus.PENDING_APPROVAL, req.getStatus());
            assertNull(req.getDecidedBy());
            verify(notificationService).createForUser(eq(1L), anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("throws ValidationException when request is not rejected")
        void throwsWhenNotRejected() {
            AbsenceRequest req = buildPersistedRequest(AbsenceStatus.APPROVED);
            when(absenceRequestRepository.findByIdWithAssociations(100)).thenReturn(Optional.of(req));

            assertThrows(ValidationException.class,
                    () -> absenceService.cancelRejectedAbsence(100, null, 2L, Set.of("ROLE_HR_MANAGER")));
            verify(absenceRequestRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws ValidationException when non-approver manager tries to cancel the rejection")
        void throwsWhenNotAuthorized() {
            AbsenceRequest req = buildPersistedRequest(AbsenceStatus.REJECTED);
            req.setApprovers(new HashSet<>());
            when(absenceRequestRepository.findByIdWithAssociations(100)).thenReturn(Optional.of(req));

            assertThrows(ValidationException.class,
                    () -> absenceService.cancelRejectedAbsence(100, null, 99L, Set.of("ROLE_MANAGER")));
            verify(absenceRequestRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteAbsence")
    class DeleteAbsenceTests {

        @Test
        @DisplayName("deletes pending request owned by user")
        void deletesSuccessfully() {
            AbsenceRequest req = buildPersistedRequest(AbsenceStatus.PENDING_APPROVAL);
            when(absenceRequestRepository.findByIdWithAssociations(100)).thenReturn(Optional.of(req));

            absenceService.deleteAbsence(100, 1L);

            verify(absenceRequestRepository).delete(req);
        }

        @Test
        @DisplayName("throws ValidationException when user is not the owner")
        void throwsWhenNotOwner() {
            AbsenceRequest req = buildPersistedRequest(AbsenceStatus.PENDING_APPROVAL);
            when(absenceRequestRepository.findByIdWithAssociations(100)).thenReturn(Optional.of(req));

            assertThrows(ValidationException.class,
                    () -> absenceService.deleteAbsence(100, 99L));
            verify(absenceRequestRepository, never()).delete(any());
        }

        @Test
        @DisplayName("throws ValidationException when request is not pending")
        void throwsWhenNotPending() {
            AbsenceRequest req = buildPersistedRequest(AbsenceStatus.APPROVED);
            when(absenceRequestRepository.findByIdWithAssociations(100)).thenReturn(Optional.of(req));

            assertThrows(ValidationException.class,
                    () -> absenceService.deleteAbsence(100, 1L));
            verify(absenceRequestRepository, never()).delete(any());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when request does not exist")
        void throwsWhenNotFound() {
            when(absenceRequestRepository.findByIdWithAssociations(999)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> absenceService.deleteAbsence(999, 1L));
        }
    }

    private AbsenceRequest buildPersistedRequest(AbsenceStatus status) {
        return AbsenceRequest.builder()
                .id(100)
                .user(employee)
                .absenceType(AbsenceTypeCode.MALADE)
                .date(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(12, 0))
                .durationHours(new BigDecimal("3.00"))
                .status(status)
                .referenceCode("ABS-20260317-A1B2C3")
                .dateApplied(LocalDate.now())
                .approvers(new HashSet<>())
                .comments(new HashSet<>())
                .build();
    }
}

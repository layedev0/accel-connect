package heritage.africca.accel_connect.service;

import heritage.africca.accel_connect.dto.UpcomingBirthdayDTO;
import heritage.africca.accel_connect.entity.User;
import heritage.africca.accel_connect.repository.UserRepository;
import heritage.africca.accel_connect.service.Impl.NextcloudTalkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BirthdayNotificationScheduler")
class BirthdayNotificationSchedulerTest {

    private static final int LEAD_DAYS = 2;

    @Mock private EmployeeDetailsService employeeDetailsService;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Mock private NextcloudTalkService nextcloudTalkService;

    @InjectMocks
    private BirthdayNotificationScheduler scheduler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scheduler, "frontendUrl", "http://app.test");
        ReflectionTestUtils.setField(scheduler, "leadDays", LEAD_DAYS);
    }


    private User hrManager(Long id, String keycloakId) {
        User u = new User();
        u.setId(id);
        u.setFirstName("Rh");
        u.setLastName("Manager");
        u.setEmail("rh" + id + "@example.com");
        u.setKeycloakId(keycloakId);
        return u;
    }

    private UpcomingBirthdayDTO birthday(Long userId, String first, String last,
                                         String department, int daysUntil, Integer age) {
        return UpcomingBirthdayDTO.builder()
                .userId(userId)
                .firstName(first)
                .lastName(last)
                .departmentName(department)
                .nextBirthday(LocalDate.now().plusDays(daysUntil))
                .daysUntil(daysUntil)
                .today(daysUntil == 0)
                .turningAge(age)
                .build();
    }


    @Test
    @DisplayName("does nothing when no birthdays are due")
    void doesNothingWhenNoBirthdaysDue() {
        when(employeeDetailsService.getUpcomingBirthdays(LEAD_DAYS)).thenReturn(List.of());

        scheduler.notifyUpcomingBirthdays();

        verifyNoInteractions(userRepository, notificationService, nextcloudTalkService);
    }

    @Test
    @DisplayName("ignores birthdays that are not exactly leadDays away")
    void ignoresBirthdaysOutsideLeadDay() {
        // getUpcomingBirthdays(leadDays) may return birthdays nearer or farther than the lead
        // day; only those exactly leadDays away should trigger a notification.
        when(employeeDetailsService.getUpcomingBirthdays(LEAD_DAYS)).thenReturn(List.of(
                birthday(10L, "Alice", "Diallo", "IT", LEAD_DAYS - 1, 30),
                birthday(11L, "Binta", "Sow", "IT", LEAD_DAYS + 1, 28)));

        scheduler.notifyUpcomingBirthdays();

        verifyNoInteractions(userRepository, notificationService, nextcloudTalkService);
    }

    @Test
    @DisplayName("does nothing when there are no HR managers to notify")
    void doesNothingWhenNoHrManagers() {
        when(employeeDetailsService.getUpcomingBirthdays(LEAD_DAYS)).thenReturn(List.of(
                birthday(10L, "Alice", "Diallo", "IT", LEAD_DAYS, 30)));
        when(userRepository.findByRolesIn(List.of("HR_MANAGER"))).thenReturn(List.of());

        scheduler.notifyUpcomingBirthdays();

        verifyNoInteractions(notificationService, nextcloudTalkService);
    }

    @Test
    @DisplayName("notifies the HR manager via bell and omni chat when a birthday is exactly leadDays away")
    void notifiesBellAndOmniChat() {
        when(employeeDetailsService.getUpcomingBirthdays(LEAD_DAYS)).thenReturn(List.of(
                birthday(10L, "Alice", "Diallo", "IT", LEAD_DAYS, 30)));
        when(userRepository.findByRolesIn(List.of("HR_MANAGER")))
                .thenReturn(List.of(hrManager(1L, "kc-uuid-123")));

        scheduler.notifyUpcomingBirthdays();

        verify(notificationService).createForUser(eq(1L), anyString(), anyString(), eq("http://app.test/"));
        verify(nextcloudTalkService, timeout(1000))
                .sendDirectMessage(eq("SSO-kc-uuid-123"), anyString());
    }

    @Test
    @DisplayName("creates the in-app notification but skips omni chat when HR manager has no Keycloak ID")
    void skipsOmniChatWhenNoKeycloakId() {
        when(employeeDetailsService.getUpcomingBirthdays(LEAD_DAYS)).thenReturn(List.of(
                birthday(10L, "Alice", "Diallo", "IT", LEAD_DAYS, 30)));
        when(userRepository.findByRolesIn(List.of("HR_MANAGER")))
                .thenReturn(List.of(hrManager(1L, null)));

        scheduler.notifyUpcomingBirthdays();

        verify(notificationService).createForUser(eq(1L), anyString(), anyString(), anyString());
        verifyNoInteractions(nextcloudTalkService);
    }

    @Test
    @DisplayName("sends one digest per HR manager listing every celebrant due that day")
    void sendsOneDigestPerManagerWithAllCelebrants() {
        when(employeeDetailsService.getUpcomingBirthdays(LEAD_DAYS)).thenReturn(List.of(
                birthday(10L, "Alice", "Diallo", "IT", LEAD_DAYS, 30),
                birthday(11L, "Binta", "Sow", "Finance", LEAD_DAYS, 28)));
        when(userRepository.findByRolesIn(List.of("HR_MANAGER")))
                .thenReturn(List.of(hrManager(1L, "kc-1"), hrManager(2L, "kc-2")));

        scheduler.notifyUpcomingBirthdays();

        // exactly one bell notification per manager
        verify(notificationService, times(1)).createForUser(eq(1L), anyString(), anyString(), anyString());
        verify(notificationService, times(1)).createForUser(eq(2L), anyString(), anyString(), anyString());
        // the digest message lists both celebrants
        verify(notificationService).createForUser(eq(1L), anyString(),
                argThat(msg -> msg.contains("Alice Diallo") && msg.contains("Binta Sow")), anyString());
        // omni chat message includes both names, department and the link
        verify(nextcloudTalkService, timeout(1000)).sendDirectMessage(eq("SSO-kc-1"),
                argThat(msg -> msg.contains("Alice Diallo") && msg.contains("Binta Sow")
                        && msg.contains("Finance") && msg.contains("http://app.test/")));
    }
}

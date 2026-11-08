package heritage.africca.accel_connect.service;

import heritage.africca.accel_connect.dto.UserLeaveBalanceDto;
import heritage.africca.accel_connect.entity.LeaveBalance;
import heritage.africca.accel_connect.entity.User;
import heritage.africca.accel_connect.repository.LeaveBalanceRepository;
import heritage.africca.accel_connect.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LeaveBalanceServiceTest {

    @Mock
    private LeaveBalanceRepository leaveBalanceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private LeaveBalanceService leaveBalanceService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testIncrementMonthlyBalance() {

        LeaveBalance balance1 = new LeaveBalance(1L, null, BigDecimal.valueOf(10), BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.now());
        LeaveBalance balance2 = new LeaveBalance(2L, null, BigDecimal.valueOf(15), BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.now());
        when(leaveBalanceRepository.findAll()).thenReturn(List.of(balance1, balance2));

        leaveBalanceService.incrementMonthlyBalance();

        ArgumentCaptor<LeaveBalance> captor = ArgumentCaptor.forClass(LeaveBalance.class);
        verify(leaveBalanceRepository, times(2)).save(captor.capture());

        List<LeaveBalance> savedBalances = captor.getAllValues();
        assertEquals(BigDecimal.valueOf(12), savedBalances.get(0).getBalanceDays());
        assertEquals(BigDecimal.valueOf(17), savedBalances.get(1).getBalanceDays());
    }

    @Test
    void testAddBalanceForUser() {
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        LeaveBalance existingBalance = new LeaveBalance(1L, user, BigDecimal.valueOf(10), BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.now());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(leaveBalanceRepository.findByUser_Id(userId)).thenReturn(Optional.of(existingBalance));

        leaveBalanceService.addBalanceForUser(userId, BigDecimal.valueOf(5));

        ArgumentCaptor<LeaveBalance> captor = ArgumentCaptor.forClass(LeaveBalance.class);
        verify(leaveBalanceRepository).save(captor.capture());

        LeaveBalance updatedBalance = captor.getValue();
        assertEquals(BigDecimal.valueOf(15), updatedBalance.getBalanceDays());
        assertEquals(LocalDate.now(), updatedBalance.getLastUpdated());
    }

    @Test
    void testAddBalanceForUser_CreatesBalanceWhenNotExists() {

        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(leaveBalanceRepository.findByUser_Id(userId)).thenReturn(Optional.empty());

        leaveBalanceService.addBalanceForUser(userId, BigDecimal.valueOf(8));

        ArgumentCaptor<LeaveBalance> captor = ArgumentCaptor.forClass(LeaveBalance.class);
        verify(leaveBalanceRepository).save(captor.capture());

        LeaveBalance createdBalance = captor.getValue();
        assertEquals(user, createdBalance.getUser());
        assertEquals(BigDecimal.valueOf(8), createdBalance.getBalanceDays());
        assertEquals(LocalDate.now(), createdBalance.getLastUpdated());
    }

    @Test
    void testAddBalanceForUser_UserNotFound() {
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                leaveBalanceService.addBalanceForUser(userId, BigDecimal.valueOf(5)));
        assertEquals("User not found: 1", exception.getMessage());
    }

    @Test
    void testListAllBalances_ReturnsAllBalances() {
        User user1 = new User();
        user1.setId(1L);
        user1.setFirstName("John");
        user1.setLastName("Doe");
        user1.setEmail("john@example.com");
        user1.setUsername("johndoe");

        User user2 = new User();
        user2.setId(2L);
        user2.setFirstName("Jane");
        user2.setLastName("Smith");
        user2.setEmail("jane@example.com");
        user2.setUsername("janesmith");

        LeaveBalance balance1 = new LeaveBalance(1L, user1, BigDecimal.valueOf(10), BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.now());
        LeaveBalance balance2 = new LeaveBalance(2L, user2, BigDecimal.valueOf(15), BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.now());
        when(leaveBalanceRepository.findAll()).thenReturn(List.of(balance1, balance2));

        List<UserLeaveBalanceDto> result = leaveBalanceService.listAllBalances();

        assertNotNull(result);
        assertEquals(2, result.size());

        UserLeaveBalanceDto dto1 = result.get(0);
        assertEquals(1L, dto1.getUserId());
        assertEquals("John Doe", dto1.getUserName());
        assertEquals("john@example.com", dto1.getUserEmail());
        assertEquals(BigDecimal.valueOf(10), dto1.getBalanceDays());
        assertEquals(LocalDate.now(), dto1.getLastUpdated());

        UserLeaveBalanceDto dto2 = result.get(1);
        assertEquals(2L, dto2.getUserId());
        assertEquals("Jane Smith", dto2.getUserName());
        assertEquals("jane@example.com", dto2.getUserEmail());
        assertEquals(BigDecimal.valueOf(15), dto2.getBalanceDays());
    }

    @Test
    void testListAllBalances_ReturnsEmptyWhenNoBalances() {
        when(leaveBalanceRepository.findAll()).thenReturn(List.of());

        List<UserLeaveBalanceDto> result = leaveBalanceService.listAllBalances();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetBalanceForUser_ReturnsBalanceWhenExists() {
        // Arrange
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        user.setFirstName("Alice");
        user.setLastName("Brown");
        user.setEmail("alice@example.com");

        LeaveBalance balance = new LeaveBalance(1L, user, BigDecimal.valueOf(12.5), BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.now());
        when(leaveBalanceRepository.findByUser_Id(userId)).thenReturn(Optional.of(balance));

        UserLeaveBalanceDto result = leaveBalanceService.getBalanceForUser(userId);

        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertEquals("Alice Brown", result.getUserName());
        assertEquals("alice@example.com", result.getUserEmail());
        assertEquals(BigDecimal.valueOf(12.5), result.getBalanceDays());
        assertEquals(LocalDate.now(), result.getLastUpdated());
    }

    @Test
    void testGetBalanceForUser_ReturnsNullWhenNotExists() {
        Long userId = 999L;
        when(leaveBalanceRepository.findByUser_Id(userId)).thenReturn(Optional.empty());

        UserLeaveBalanceDto result = leaveBalanceService.getBalanceForUser(userId);

        assertNull(result);
    }

    @Test
    void testGetBalanceForUser_UsesUsernameWhenNameMissing() {
        // Arrange
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setEmail("test@example.com");

        LeaveBalance balance = new LeaveBalance(1L, user, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.now());
        when(leaveBalanceRepository.findByUser_Id(userId)).thenReturn(Optional.of(balance));

        UserLeaveBalanceDto result = leaveBalanceService.getBalanceForUser(userId);

        assertNotNull(result);
        assertEquals("testuser", result.getUserName());
    }

    @Test
    void testDeductBalanceForUser() {

        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        LeaveBalance existingBalance = new LeaveBalance(1L, user, BigDecimal.valueOf(15), BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.now());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(leaveBalanceRepository.findByUser_Id(userId)).thenReturn(Optional.of(existingBalance));

        leaveBalanceService.deductBalanceForUser(userId, BigDecimal.valueOf(5));

        ArgumentCaptor<LeaveBalance> captor = ArgumentCaptor.forClass(LeaveBalance.class);
        verify(leaveBalanceRepository).save(captor.capture());
        LeaveBalance saved = captor.getValue();
        assertEquals(BigDecimal.valueOf(10), saved.getBalanceDays());
    }

    @Test
    void testDeductBalanceForUser_GoesNegative() {

        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        LeaveBalance existingBalance = new LeaveBalance(1L, user, BigDecimal.valueOf(3), BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.now());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(leaveBalanceRepository.findByUser_Id(userId)).thenReturn(Optional.of(existingBalance));


        leaveBalanceService.deductBalanceForUser(userId, BigDecimal.valueOf(10));


        ArgumentCaptor<LeaveBalance> captor = ArgumentCaptor.forClass(LeaveBalance.class);
        verify(leaveBalanceRepository).save(captor.capture());
        LeaveBalance saved = captor.getValue();
        assertEquals(BigDecimal.valueOf(-7), saved.getBalanceDays());
    }

    @Test
    void testDeductBalanceForUser_CreatesBalanceWhenNotExists() {

        Long userId = 1L;
        User user = new User();
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(leaveBalanceRepository.findByUser_Id(userId)).thenReturn(Optional.empty());


        leaveBalanceService.deductBalanceForUser(userId, BigDecimal.valueOf(5));


        ArgumentCaptor<LeaveBalance> captor = ArgumentCaptor.forClass(LeaveBalance.class);
        verify(leaveBalanceRepository).save(captor.capture());
        LeaveBalance saved = captor.getValue();
        assertEquals(user, saved.getUser());
        assertEquals(BigDecimal.valueOf(-5), saved.getBalanceDays());
        assertEquals(LocalDate.now(), saved.getLastUpdated());
    }

    @Test
    void testDeductBalanceForUser_UserNotFound() {

        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());


        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                leaveBalanceService.deductBalanceForUser(userId, BigDecimal.ONE));
        assertEquals("User not found: 1", exception.getMessage());
    }

    @Test
    void testAddLeaveDue_NotifiesUserWithOldAndNewBalance() {
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        LeaveBalance existingBalance = new LeaveBalance(1L, user, BigDecimal.valueOf(10), BigDecimal.valueOf(15), BigDecimal.valueOf(5), LocalDate.now());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(leaveBalanceRepository.findByUser_Id(userId)).thenReturn(Optional.of(existingBalance));

        leaveBalanceService.addLeaveDue(userId, BigDecimal.valueOf(3));

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).createForUser(eq(userId), eq("Solde de congés mis à jour"), messageCaptor.capture(), anyString());
        String message = messageCaptor.getValue();
        assertTrue(message.contains("10 j"), "should mention the previous balance");
        assertTrue(message.contains("13 j"), "should mention the new balance");
    }

    @Test
    void testDeductLeaveTaken_NotifiesUserWithOldAndNewBalance() {
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        LeaveBalance existingBalance = new LeaveBalance(1L, user, BigDecimal.valueOf(8), BigDecimal.valueOf(15), BigDecimal.valueOf(7), LocalDate.now());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(leaveBalanceRepository.findByUser_Id(userId)).thenReturn(Optional.of(existingBalance));

        leaveBalanceService.deductLeaveTaken(userId, BigDecimal.valueOf(2));

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).createForUser(eq(userId), eq("Solde de congés mis à jour"), messageCaptor.capture(), anyString());
        String message = messageCaptor.getValue();
        assertTrue(message.contains("8 j"), "should mention the previous balance");
        assertTrue(message.contains("10 j"), "should mention the new balance");
    }
}
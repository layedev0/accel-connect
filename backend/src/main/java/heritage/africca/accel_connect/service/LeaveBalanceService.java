package heritage.africca.accel_connect.service;

import heritage.africca.accel_connect.dto.UserLeaveBalanceDto;
import heritage.africca.accel_connect.entity.LeaveBalance;
import heritage.africca.accel_connect.entity.User;
import heritage.africca.accel_connect.repository.LeaveBalanceRepository;
import heritage.africca.accel_connect.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LeaveBalanceService {

    private final LeaveBalanceRepository leaveBalanceRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    public LeaveBalanceService(LeaveBalanceRepository leaveBalanceRepository,
                               UserRepository userRepository,
                               NotificationService notificationService,
                               EmailService emailService) {
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.emailService = emailService;
    }

    @Transactional(readOnly = true)
    public List<UserLeaveBalanceDto> listAllBalances() {
        return leaveBalanceRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserLeaveBalanceDto getBalanceForUser(Long userId) {
        return leaveBalanceRepository.findByUser_Id(userId)
                .map(this::toDto)
                .orElse(null);
    }

    private UserLeaveBalanceDto toDto(LeaveBalance lb) {
        User u = lb.getUser();
        String name = (u != null && u.getFirstName() != null && u.getLastName() != null)
                ? u.getFirstName() + " " + u.getLastName()
                : (u != null ? u.getUsername() : "—");
        String email = (u != null && u.getEmail() != null) ? u.getEmail() : "";
        Long uid = (u != null && u.getId() != null) ? u.getId() : null;

        BigDecimal remaining = lb.getBalanceDays() != null ? lb.getBalanceDays() : BigDecimal.ZERO;
        BigDecimal taken = lb.getLeaveTaken() != null ? lb.getLeaveTaken() : BigDecimal.ZERO;
        BigDecimal due = lb.getLeaveDue() != null ? lb.getLeaveDue() : BigDecimal.ZERO;

        return UserLeaveBalanceDto.builder()
                .userId(uid)
                .userName(name)
                .userEmail(email)
                .balanceDays(remaining)
                .lastUpdated(lb.getLastUpdated())
                .leaveDue(due)
                .leaveTaken(taken)
                .leaveBalance(remaining)
                .build();
    }

    @Scheduled(cron = "0 0 0 1 * ?") // Runs on the 1st day of every month at midnight
    @Transactional
    public void incrementMonthlyBalance() {
        log.info("Running monthly leave balance increment task");
        List<LeaveBalance> balances = leaveBalanceRepository.findAll();
        for (LeaveBalance balance : balances) {
            balance.setBalanceDays(balance.getBalanceDays().add(BigDecimal.valueOf(2)));
            balance.setLeaveDue(balance.getLeaveDue().add(BigDecimal.valueOf(2)));
            balance.setLastUpdated(LocalDate.now());
            leaveBalanceRepository.save(balance);
            log.info("Updated leave balance for user {}: new balance = {}, new leave due = {}",
                    balance.getUser() != null ? balance.getUser().getEmail() : "unknown",
                    balance.getBalanceDays(),
                    balance.getLeaveDue());
        }
    }

    public void addBalanceForUser(Long userId, BigDecimal days) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        LeaveBalance balance = leaveBalanceRepository.findByUser_Id(userId)
                .orElseGet(() -> {
                    LeaveBalance newBalance = new LeaveBalance();
                    newBalance.setUser(user);
                    newBalance.setBalanceDays(BigDecimal.ZERO);
                    newBalance.setLastUpdated(LocalDate.now());
                    return newBalance;
                });

        balance.setBalanceDays(balance.getBalanceDays().add(days));
        balance.setLeaveDue(balance.getLeaveDue().add(days));
        balance.setLastUpdated(LocalDate.now());
        leaveBalanceRepository.save(balance);
    }


    public void deductBalanceForUser(Long userId, BigDecimal days) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        LeaveBalance balance = leaveBalanceRepository.findByUser_Id(userId)
                .orElseGet(() -> {
                    LeaveBalance newBalance = new LeaveBalance();
                    newBalance.setUser(user);
                    newBalance.setBalanceDays(BigDecimal.ZERO);
                    newBalance.setLastUpdated(LocalDate.now());
                    return newBalance;
                });

        BigDecimal newBalanceDays = balance.getBalanceDays().subtract(days);
        balance.setBalanceDays(newBalanceDays);
        balance.setLeaveTaken(balance.getLeaveTaken().add(days));
        balance.setLastUpdated(LocalDate.now());
        leaveBalanceRepository.save(balance);
    }

    public void addLeaveDue(Long userId, BigDecimal days) {
        LeaveBalance balance = getOrCreate(userId);
        BigDecimal oldBalance = balance.getBalanceDays();
        balance.setLeaveDue(balance.getLeaveDue().add(days));
        balance.setBalanceDays(balance.getLeaveDue().subtract(balance.getLeaveTaken()).max(BigDecimal.ZERO));
        balance.setLastUpdated(LocalDate.now());
        leaveBalanceRepository.save(balance);
        notifyBalanceAdjusted(balance, "Congés dû", "+" + days + " jour(s)", oldBalance);
    }

    public void deductLeaveDue(Long userId, BigDecimal days) {
        LeaveBalance balance = getOrCreate(userId);
        BigDecimal oldBalance = balance.getBalanceDays();
        balance.setLeaveDue(balance.getLeaveDue().subtract(days).max(BigDecimal.ZERO));
        balance.setBalanceDays(balance.getLeaveDue().subtract(balance.getLeaveTaken()).max(BigDecimal.ZERO));
        balance.setLastUpdated(LocalDate.now());
        leaveBalanceRepository.save(balance);
        notifyBalanceAdjusted(balance, "Congés dû", "-" + days + " jour(s)", oldBalance);
    }

    public void addLeaveTaken(Long userId, BigDecimal days) {
        LeaveBalance balance = getOrCreate(userId);
        BigDecimal oldBalance = balance.getBalanceDays();
        balance.setLeaveTaken(balance.getLeaveTaken().add(days));
        balance.setBalanceDays(balance.getLeaveDue().subtract(balance.getLeaveTaken()).max(BigDecimal.ZERO));
        balance.setLastUpdated(LocalDate.now());
        leaveBalanceRepository.save(balance);
        notifyBalanceAdjusted(balance, "Congés pris", "+" + days + " jour(s)", oldBalance);
    }

    public void deductLeaveTaken(Long userId, BigDecimal days) {
        LeaveBalance balance = getOrCreate(userId);
        BigDecimal oldBalance = balance.getBalanceDays();
        balance.setLeaveTaken(balance.getLeaveTaken().subtract(days).max(BigDecimal.ZERO));
        balance.setBalanceDays(balance.getLeaveDue().subtract(balance.getLeaveTaken()).max(BigDecimal.ZERO));
        balance.setLastUpdated(LocalDate.now());
        leaveBalanceRepository.save(balance);
        notifyBalanceAdjusted(balance, "Congés pris", "-" + days + " jour(s)", oldBalance);
    }

    private void notifyBalanceAdjusted(LeaveBalance balance, String fieldLabel, String changeLabel, BigDecimal oldBalance) {
        User user = balance.getUser();
        if (user == null || user.getId() == null) {
            return;
        }
        notificationService.createForUser(
                user.getId(),
                "Solde de congés mis à jour",
                changeLabel + " sur vos " + fieldLabel.toLowerCase() + ". Solde congés : "
                        + oldBalance + " j → " + balance.getBalanceDays() + " j.",
                frontendUrl + "/rh/conges"
        );
        emailService.notifyEmployeeOfBalanceAdjustment(
                user, fieldLabel, changeLabel, oldBalance.toString(), balance.getBalanceDays().toString());
    }

    private LeaveBalance getOrCreate(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        return leaveBalanceRepository.findByUser_Id(userId)
                .orElseGet(() -> {
                    LeaveBalance lb = new LeaveBalance();
                    lb.setUser(user);
                    lb.setBalanceDays(BigDecimal.ZERO);
                    lb.setLeaveDue(BigDecimal.ZERO);
                    lb.setLeaveTaken(BigDecimal.ZERO);
                    lb.setLastUpdated(LocalDate.now());
                    return lb;
                });
    }
}

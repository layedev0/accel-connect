package heritage.africca.accel_connect.service.Impl;

import heritage.africca.accel_connect.dto.*;
import heritage.africca.accel_connect.entity.*;
import heritage.africca.accel_connect.repository.*;
import heritage.africca.accel_connect.service.DeliveryFeeService;
import heritage.africca.accel_connect.service.UserBalanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;

import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SERVICE DE GESTION DES SOLDES UTILISATEUR
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserBalanceServiceImpl implements UserBalanceService {

    private final UserBalanceRepository userBalanceRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final DeliveryFeeService deliveryFeeService;
    private final DepositHistoryRepository depositHistoryRepository;

    /**
     * RECUPERE LE SOLDE DE L'UTILISATEUR
     */
    @Transactional(readOnly = true)
    @Override
    public UserBalanceDTO getMyBalance(Authentication auth) {
        User user = getCurrentUser(auth);

        UserBalance balance = userBalanceRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    UserBalance newBalance = new UserBalance();
                    newBalance.setUser(user);
                    newBalance.setTotalDeposited(0.0);
                    newBalance.setTotalSpent(0.0);
                    newBalance.setBalance(0.0);
                    return newBalance;
                });

        return convertToDTO(balance);
    }

    /**
     * EFFECTUE UN DEPOT (ADMIN uniquement)
     */
    @Transactional
    @Override
    public UserBalanceDTO deposit(DepositRequest request, Authentication auth) {
        if (request.getAmount() <= 0) {
            throw new RuntimeException("Le montant doit être positif");
        }

        User admin = getCurrentUser(auth);
        User targetUser = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        UserBalance balance = userBalanceRepository.findByUserId(targetUser.getId())
                .orElseGet(() -> {
                    UserBalance newBalance = new UserBalance();
                    newBalance.setUser(targetUser);
                    return userBalanceRepository.save(newBalance);
                });

        balance.addDeposit(request.getAmount());
        UserBalance savedBalance = userBalanceRepository.save(balance);

        // Sauvegarder l'historique du depot
        DepositHistory history = new DepositHistory();
        history.setUser(targetUser);
        history.setAmount(request.getAmount());
        history.setDescription(request.getDescription());
        depositHistoryRepository.save(history);

        log.info("Depot de {} FCFA effectue par {} pour {} - Nouveau solde: {} FCFA",
                request.getAmount(), admin.getUsername(), targetUser.getUsername(),
                savedBalance.getBalance());

        return convertToDTO(savedBalance);
    }

    /**
     * AJOUTE UNE DETTE A UN UTILISATEUR (ADMIN uniquement)
     */
    @Transactional
    @Override
    public UserBalanceDTO addDebt(AddDebtRequest request, Authentication auth) {
        if (request.getDebtAmount() <= 0) {
            throw new RuntimeException("Le montant de la dette doit être positif");
        }

        User admin = getCurrentUser(auth);
        User targetUser = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        UserBalance balance = userBalanceRepository.findByUserId(targetUser.getId())
                .orElseGet(() -> {
                    UserBalance newBalance = new UserBalance();
                    newBalance.setUser(targetUser);
                    newBalance.setTotalDeposited(0.0);
                    newBalance.setTotalSpent(0.0);
                    newBalance.setBalance(0.0);
                    return userBalanceRepository.save(newBalance);
                });

        // Soustraire le montant de la dette du solde
        double oldBalance = balance.getBalance();
        balance.setBalance(oldBalance - request.getDebtAmount());

        // Ajuster totalSpent pour refleter la dette
        balance.setTotalSpent(balance.getTotalSpent() + request.getDebtAmount());

        UserBalance savedBalance = userBalanceRepository.save(balance);

        log.info("Dette de {} FCFA ajoutee par {} pour {} - Ancien solde: {} FCFA, Nouveau solde: {} FCFA, Raison: {}",
                request.getDebtAmount(), admin.getUsername(), targetUser.getUsername(),
                oldBalance, savedBalance.getBalance(), request.getReason());

        return convertToDTO(savedBalance);
    }

    /**
     * MET A JOUR LE SOLDE (ADMIN - pour remboursements effectifs)
     */
    @Transactional
    @Override
    public UserBalanceDTO updateBalance(UpdateBalanceRequest request, Authentication auth) {
        if (request.getNewBalance() == null) {
            throw new RuntimeException("Le nouveau solde doit être spécifié");
        }

        User admin = getCurrentUser(auth);
        User targetUser = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        UserBalance balance = userBalanceRepository.findByUserId(targetUser.getId())
                .orElseGet(() -> {
                    UserBalance newBalance = new UserBalance();
                    newBalance.setUser(targetUser);
                    return userBalanceRepository.save(newBalance);
                });

        double oldBalance = balance.getBalance();

        balance.setTotalDeposited(request.getNewBalance() + balance.getTotalSpent());
        balance.setBalance(request.getNewBalance());

        UserBalance savedBalance = userBalanceRepository.save(balance);

        log.info("Solde mis a jour par {} pour {} - Ancien: {} FCFA, Nouveau: {} FCFA, Raison: {}",
                admin.getUsername(), targetUser.getUsername(), oldBalance,
                savedBalance.getBalance(), request.getReason());

        return convertToDTO(savedBalance);
    }

    /**
     * RECUPERE LES STATISTIQUES DE TOUS LES UTILISATEURS (ADMIN)
     */
    @Transactional(readOnly = true)
    @Override
    public List<UserBalanceStatsDTO> getAllUserStats() {
        List<User> allUsers = (List<User>) userRepository.findAll();

        return allUsers.stream()
                .map(user -> {
                    UserBalance balance = userBalanceRepository.findByUserId(user.getId())
                            .orElseGet(() -> {
                                UserBalance newBalance = new UserBalance();
                                newBalance.setUser(user);
                                newBalance.setTotalDeposited(0.0);
                                newBalance.setTotalSpent(0.0);
                                newBalance.setBalance(0.0);
                                return newBalance;
                            });

                    Double realBalance = balance.getBalance();
                    Long orderCount = orderRepository.countByUserId(user.getId());

                    String status;
                    if (realBalance < 0) {
                        status = "Dette: " + String.format("%.0f FCFA", Math.abs(realBalance));
                    } else if (realBalance > 0) {
                        status = "À rembourser: " + String.format("%.0f FCFA", realBalance);
                    } else {
                        status = "En règle";
                    }

                    UserBalanceStatsDTO dto = new UserBalanceStatsDTO();
                    dto.setUserId(user.getId());
                    dto.setUsername(user.getUsername());
                    dto.setEmail(user.getEmail());
                    dto.setTotalDeposited(balance.getTotalDeposited());
                    dto.setTotalSpent(balance.getTotalSpent());
                    dto.setBalance(realBalance);
                    dto.setOrderCount(orderCount.intValue());
                    dto.setStatus(status);
                    dto.setHasDebt(realBalance < 0);
                    dto.setDebtAmount(realBalance < 0 ? Math.abs(realBalance) : 0.0);
                    dto.setRefundableAmount(realBalance > 0 ? realBalance : 0.0);
                    dto.setEstimatedDeliveryFees(0.0);

                    return dto;
                })
                .collect(Collectors.toList());
    }

    // ========== UTILITAIRES ==========

    private User getCurrentUser(Authentication auth) {
        String keycloakId = ((org.springframework.security.oauth2.jwt.Jwt) auth.getPrincipal()).getSubject();
        return userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }

    private UserBalanceDTO convertToDTO(UserBalance balance) {
        UserBalanceDTO dto = new UserBalanceDTO();
        dto.setUserId(balance.getUser().getId());
        dto.setUsername(balance.getUser().getUsername());
        dto.setTotalDeposited(balance.getTotalDeposited());
        dto.setTotalSpent(balance.getTotalSpent());

        Double realBalance = balance.getBalance();
        dto.setBalance(realBalance);

        dto.setEstimatedDeliveryFees(0.0);

        dto.setHasDebt(realBalance < 0);
        dto.setDebtAmount(realBalance < 0 ? Math.abs(realBalance) : 0.0);
        dto.setRefundableAmount(realBalance > 0 ? realBalance : 0.0);

        // Avoir et Dette
        dto.setAvoir(realBalance > 0 ? realBalance : 0.0);
        dto.setDebt(realBalance < 0 ? Math.abs(realBalance) : 0.0);

        // Historique des depots
        Long userId = balance.getUser().getId();
        List<DepositHistory> recentDeposits = depositHistoryRepository.findTop4ByUserIdOrderByCreatedAtDesc(userId);
        List<DepositHistoryDTO> depositDTOs = recentDeposits.stream()
                .map(d -> new DepositHistoryDTO(d.getAmount(), d.getCreatedAt(), d.getDescription()))
                .collect(Collectors.toList());
        dto.setRecentDeposits(depositDTOs);

        if (!recentDeposits.isEmpty()) {
            DepositHistory lastDeposit = recentDeposits.get(0);
            dto.setLastDepositAmount(lastDeposit.getAmount());
            dto.setLastDepositDate(lastDeposit.getCreatedAt());
        } else {
            dto.setLastDepositAmount(0.0);
            dto.setLastDepositDate(null);
        }

        // Depenses hebdomadaires
        List<Object[]> weeklyRows = orderRepository.findWeeklySpendingByUserId(userId);
        List<WeeklySpendingDTO> weeklyDTOs = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");

        LocalDate currentWeekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        double currentWeekSpent = 0.0;

        for (Object[] row : weeklyRows) {
            LocalDate weekStart;
            if (row[0] instanceof Date) {
                weekStart = ((Date) row[0]).toLocalDate();
            } else {
                weekStart = (LocalDate) row[0];
            }
            Double totalSpent = row[1] instanceof Number ? ((Number) row[1]).doubleValue() : 0.0;

            LocalDate weekEnd = weekStart.plusDays(4); // Lundi -> Vendredi
            String label = weekStart.format(fmt) + " - " + weekEnd.format(fmt);

            if (weekStart.equals(currentWeekStart)) {
                currentWeekSpent = totalSpent;
            } else {
                weeklyDTOs.add(new WeeklySpendingDTO(label, totalSpent));
            }
        }

        dto.setCurrentWeekSpent(currentWeekSpent);
        // Garder max 4 semaines precedentes
        dto.setRecentWeeksSpending(weeklyDTOs.size() > 4 ? weeklyDTOs.subList(0, 4) : weeklyDTOs);

        if (realBalance < 0) {
            dto.setMessage(String.format(
                    "Vous devez %.0f FCFA a l'admin.",
                    Math.abs(realBalance)
            ));
        } else if (realBalance > 0) {
            dto.setMessage(String.format(
                    "L'admin vous doit %.0f FCFA.",
                    realBalance
            ));
        } else {
            dto.setMessage("Votre compte est regle !");
        }

        return dto;
    }
}

package heritage.africca.accel_connect.service;

import heritage.africca.accel_connect.dto.*;
import org.springframework.security.core.Authentication;

import java.util.List;

/**
 * 💰 INTERFACE DE SERVICE DE GESTION DES SOLDES UTILISATEUR
 */
public interface UserBalanceService {

    /**
     * 📊 Récupère le solde de l'utilisateur connecté
     */
    UserBalanceDTO getMyBalance(Authentication auth);

    /**
     * 💵 Effectue un dépôt pour un utilisateur (ADMIN uniquement)
     */
    UserBalanceDTO deposit(DepositRequest request, Authentication auth);

    /**
     * 💸 Ajoute une dette à un utilisateur (ADMIN uniquement)
     */
    UserBalanceDTO addDebt(AddDebtRequest request, Authentication auth);

    /**
     * 🔄 Met à jour le solde d'un utilisateur (ADMIN uniquement)
     */
    UserBalanceDTO updateBalance(UpdateBalanceRequest request, Authentication auth);

    /**
     * 📋 Récupère les statistiques de tous les utilisateurs (ADMIN uniquement)
     */
    List<UserBalanceStatsDTO> getAllUserStats();
}
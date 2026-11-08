package heritage.africca.accel_connect.controller;

import heritage.africca.accel_connect.dto.*;
import heritage.africca.accel_connect.service.UserBalanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/balance")
@CrossOrigin("*")
@RequiredArgsConstructor
public class UserBalanceController {

    private final UserBalanceService userBalanceService;

    /**
     * 📊 RÉCUPÈRE LE SOLDE DE L'UTILISATEUR
     */
    @GetMapping
    public ResponseEntity<UserBalanceDTO> getMyBalance(Authentication auth) {
        UserBalanceDTO balance = userBalanceService.getMyBalance(auth);
        return ResponseEntity.ok(balance);
    }

    /**
     * 💵 EFFECTUE UN DÉPÔT (ADMIN uniquement)
     */
    @PostMapping("/deposit")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<UserBalanceDTO> deposit(
            @Valid @RequestBody DepositRequest request,
            Authentication auth
    ) {
        UserBalanceDTO balance = userBalanceService.deposit(request, auth);
        return ResponseEntity.ok(balance);
    }

    /**
     * 💸 AJOUTE UNE DETTE À UN UTILISATEUR (ADMIN uniquement)
     */
    @PostMapping("/add-debt")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<UserBalanceDTO> addDebt(
            @Valid @RequestBody AddDebtRequest request,
            Authentication auth
    ) {
        UserBalanceDTO balance = userBalanceService.addDebt(request, auth);
        return ResponseEntity.ok(balance);
    }

    /**
     * 🔄 MET À JOUR LE SOLDE (ADMIN uniquement)
     * Utilisé pour ajuster le solde après un remboursement effectif
     */
    @PutMapping("/update")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<UserBalanceDTO> updateBalance(
            @Valid @RequestBody UpdateBalanceRequest request,
            Authentication auth
    ) {
        UserBalanceDTO balance = userBalanceService.updateBalance(request, auth);
        return ResponseEntity.ok(balance);
    }

    /**
     * 📋 RÉCUPÈRE LES STATS DE TOUS LES UTILISATEURS (ADMIN)
     */
    @GetMapping("/admin/stats")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<List<UserBalanceStatsDTO>> getAllUserStats() {
        List<UserBalanceStatsDTO> stats = userBalanceService.getAllUserStats();
        return ResponseEntity.ok(stats);
    }
}
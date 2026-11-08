package heritage.africca.accel_connect.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Représente le budget hebdomadaire d'un utilisateur
 * Période: Lundi au Vendredi
 */
@Entity
@Table(name = "weekly_budgets")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyBudget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate weekStartDate;  // Lundi

    @Column(nullable = false)
    private LocalDate weekEndDate;    // Vendredi

    /**
     * Montant déposé par l'utilisateur cette semaine
     * C'est ce que l'utilisateur DONNE à l'admin
     */
    @Column(nullable = false)
    private Double depositedAmount = 0.0;

    /**
     * Montant RÉELLEMENT dépensé par l'utilisateur
     * C'est ce qui est débité APRÈS application de la subvention
     * Ex: Commande de 1500 FCFA → Subvention 1000 → spentAmount = 500
     */
    @Column(nullable = false)
    private Double spentAmount = 0.0;

    /**
     * Frais de livraison calculés automatiquement le vendredi
     */
    @Column(nullable = false)
    private Double deliveryFees = 0.0;

    /**
     * Frais de livraison calculés automatiquement
     * Ajoutés par le système le vendredi à 15h
     */
    @Column(nullable = false)
    private Double calculatedDeliveryFees = 0.0;

    @Column(nullable = false)
    private Boolean deliveryFeesCalculated = false;

    private LocalDateTime deliveryFeesCalculatedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * CALCUL DU SOLDE CORRIGÉ
     * Balance = Ce que l'utilisateur a donné - Ce qu'il a dépensé - Les frais de livraison
     */
    public Double getBalance() {
        return depositedAmount - spentAmount - calculatedDeliveryFees;
    }

    /**
     * Vérifie si l'utilisateur est en dette
     */
    public boolean isInDebt() {
        return getBalance() < 0;
    }

    /**
     * Montant de la dette (valeur absolue si négatif)
     */
    public Double getDebtAmount() {
        return isInDebt() ? Math.abs(getBalance()) : 0.0;
    }

    /**
     * Montant disponible (0 si en dette)
     */
    public Double getAvailableAmount() {
        return Math.max(0, getBalance());
    }

    /**
     * Montant remboursable si la semaine se termine avec un solde positif
     */
    public Double getRefundableAmount() {
        LocalDate now = LocalDate.now();
        boolean weekEnded = now.isAfter(weekEndDate);
        return weekEnded && getBalance() > 0 ? getBalance() : 0.0;
    }

    /**
     * Ajoute un dépôt
     */
    public void addDeposit(Double amount) {
        this.depositedAmount += amount;
    }

    /**
     * Ajoute une dépense
     * Le montant passé ici est déjà APRÈS subvention
     */
    public void addSpending(Double amount) {
        this.spentAmount += amount;
    }

    /**
     * Met à jour les frais de livraison calculés
     */
    public void setCalculatedDeliveryFees(Double fees) {
        this.calculatedDeliveryFees = fees;
        this.deliveryFeesCalculated = true;
        this.deliveryFeesCalculatedAt = LocalDateTime.now();
    }
}
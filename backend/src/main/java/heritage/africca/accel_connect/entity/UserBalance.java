package heritage.africca.accel_connect.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 💰 SOLDE UTILISATEUR
 *
 * Suit les finances de l'utilisateur:
 * - totalDeposited: Argent donné à l'admin
 * - totalSpent: Argent dépensé (commandes + frais de livraison)
 * - balance: Différence (positif = admin doit rembourser, négatif = dette)
 */
@Entity
@Table(name = "user_balances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /**
     * 💵 Total DÉPOSÉ (argent donné à l'admin)
     */
    @Column(name = "total_deposited", nullable = false)
    private Double totalDeposited = 0.0;

    /**
     * 💳 Total DÉPENSÉ (commandes + frais livraison)
     */
    @Column(name = "total_spent", nullable = false)
    private Double totalSpent = 0.0;

    /**
     * ⚖️ SOLDE
     * = totalDeposited - totalSpent
     *
     * > 0 : Admin doit rembourser
     * = 0 : Réglé
     * < 0 : Utilisateur a une dette
     */
    @Column(name = "balance", nullable = false)
    private Double balance = 0.0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        this.balance = this.totalDeposited - this.totalSpent;
    }

    /**
     * 💰 Ajoute un dépôt
     */
    public void addDeposit(Double amount) {
        if (amount > 0) {
            this.totalDeposited += amount;
            this.balance = this.totalDeposited - this.totalSpent;
        }
    }

    /**
     * 💸 Ajoute une dépense
     */
    public void addExpense(Double amount) {
        if (amount > 0) {
            this.totalSpent += amount;
            this.balance = this.totalDeposited - this.totalSpent;
        }
    }

    /**
     * 🔄 Rembourse une dépense (annulation)
     */
    public void refundExpense(Double amount) {
        if (amount > 0) {
            this.totalSpent = Math.max(0, this.totalSpent - amount);
            this.balance = this.totalDeposited - this.totalSpent;
        }
    }

    /**
     * ✅ A un solde positif ?
     */
    public boolean hasPositiveBalance() {
        return this.balance > 0;
    }

    /**
     * ⚠️ A une dette ?
     */
    public boolean hasDebt() {
        return this.balance < 0;
    }

    /**
     * 💰 Montant de la dette
     */
    public Double getDebtAmount() {
        return hasDebt() ? Math.abs(this.balance) : 0.0;
    }

    /**
     * 💵 Montant remboursable
     */
    public Double getRefundableAmount() {
        return hasPositiveBalance() ? this.balance : 0.0;
    }
}
import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, tap } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import { WeeklyBudget, UserBudgetStats } from '../../model/weekly-budget.model';

@Injectable({
  providedIn: 'root',
})
export class WeeklyBudgetService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/budget`;

  // Signal réactif pour le budget actuel
  private readonly budgetSubject = new BehaviorSubject<WeeklyBudget | null>(null);
  public budget$ = this.budgetSubject.asObservable();

  // Signals pour les statistiques
  weeklyDeposit = signal(0);
  weeklySpent = signal(0);
  remainingBalance = signal(0);
  isInDebt = signal(false);

  constructor() {
    this.loadCurrentBudget();
  }

  /**
   * Charge le budget de la semaine en cours
   */
  loadCurrentBudget(): void {
    this.getCurrentWeekBudget().subscribe({
      next: (budget) => {
        this.budgetSubject.next(budget);
        this.updateSignals(budget);
        console.log('💰 Budget hebdomadaire chargé:', budget);
      },
      error: (error) => {
        console.error('❌ Erreur lors du chargement du budget:', error);
      },
    });
  }

  /**
   * Récupère le budget de la semaine en cours
   */
  getCurrentWeekBudget(): Observable<WeeklyBudget> {
    return this.http.get<WeeklyBudget>(`${this.apiUrl}/current`).pipe(
      tap((budget) => {
        this.budgetSubject.next(budget);
        this.updateSignals(budget);
      })
    );
  }

  /**
   * Récupère l'historique des budgets
   */
  getBudgetHistory(): Observable<WeeklyBudget[]> {
    return this.http.get<WeeklyBudget[]>(`${this.apiUrl}/history`);
  }

  /**
   * Met à jour les signals avec le budget
   */
  private updateSignals(budget: WeeklyBudget): void {
    this.weeklyDeposit.set(budget.depositedAmount);
    this.weeklySpent.set(budget.spentAmount + budget.deliveryFees);
    this.remainingBalance.set(budget.balance);
    this.isInDebt.set(budget.isInDebt);
  }

  /**
   * Obtient les statistiques formatées pour l'affichage
   */
  getUserBudgetStats(): UserBudgetStats {
    const budget = this.budgetSubject.value;

    if (!budget) {
      return {
        weeklyDeposit: 0,
        weeklySpent: 0,
        remainingBalance: 0,
        isInDebt: false,
        statusMessage: 'Chargement...',
      };
    }

    let statusMessage: string;
    if (budget.isInDebt) {
      statusMessage = `Vous devez ${this.formatAmount(budget.debtAmount)}`;
    } else if (budget.balance > 5000) {
      statusMessage = 'Solde suffisant';
    } else if (budget.balance > 0) {
      statusMessage = 'Pensez à recharger';
    } else {
      statusMessage = 'Solde épuisé';
    }

    return {
      weeklyDeposit: budget.depositedAmount,
      weeklySpent: budget.spentAmount + budget.deliveryFees,
      remainingBalance: budget.balance,
      isInDebt: budget.isInDebt,
      statusMessage,
    };
  }

  /**
   * Formate un montant en K (milliers)
   */
  formatBalance(amount: number): string {
    if (amount >= 1000) {
      return Math.floor(amount / 1000) + 'K';
    }
    return amount.toString();
  }

  /**
   * Formate un montant en FCFA
   */
  formatAmount(amount: number): string {
    return (
      new Intl.NumberFormat('fr-FR', {
        style: 'decimal',
        minimumFractionDigits: 0,
        maximumFractionDigits: 0,
      }).format(amount) + ' FCFA'
    );
  }

  /**
   * Obtient le budget actuel depuis le BehaviorSubject
   */
  getCurrentBudget(): WeeklyBudget | null {
    return this.budgetSubject.value;
  }
}

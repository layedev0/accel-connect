import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, tap } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import { Transaction, TransactionType, Wallet, WalletBalance } from '../../model/wallet.model';

@Injectable({
  providedIn: 'root',
})
export class WalletService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/wallet`;

  // Signal réactif pour le solde
  private readonly balanceSubject = new BehaviorSubject<WalletBalance | null>(null);
  public balance$ = this.balanceSubject.asObservable();

  // Signal pour le solde actuel
  currentBalance = signal(0);

  constructor() {
    this.loadBalance();
  }

  /**
   * Charge le solde de l'utilisateur
   */
  loadBalance(): void {
    this.getBalance().subscribe({
      next: (balance) => {
        this.balanceSubject.next(balance);
        this.currentBalance.set(balance.balance);
        console.log('💰 Solde chargé:', balance);
      },
      error: (error) => {
        console.error('❌ Erreur lors du chargement du solde:', error);
      },
    });
  }

  /**
   * Récupère le wallet complet
   */
  getWallet(): Observable<Wallet> {
    return this.http.get<Wallet>(this.apiUrl);
  }

  /**
   * Récupère le solde détaillé
   */
  getBalance(): Observable<WalletBalance> {
    return this.http.get<WalletBalance>(`${this.apiUrl}/balance`).pipe(
      tap((balance) => {
        this.balanceSubject.next(balance);
        this.currentBalance.set(balance.balance);
      })
    );
  }

  /**
   * Récupère l'historique des transactions
   */
  getTransactionHistory(): Observable<Transaction[]> {
    return this.http.get<Transaction[]>(`${this.apiUrl}/transactions`);
  }

  /**
   * Récupère le solde actuel depuis le BehaviorSubject
   */
  getCurrentBalance(): WalletBalance | null {
    return this.balanceSubject.value;
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
   * Obtient la couleur du type de transaction
   */
  getTransactionColor(type: TransactionType): string {
    switch (type) {
      case TransactionType.CREDIT:
        return 'text-green-600 bg-green-50';
      case TransactionType.DEBIT:
        return 'text-red-600 bg-red-50';
      case TransactionType.REFUND:
        return 'text-blue-600 bg-blue-50';
      default:
        return 'text-gray-600 bg-gray-50';
    }
  }

  /**
   * Obtient le label du type de transaction
   */
  getTransactionLabel(type: TransactionType): string {
    switch (type) {
      case TransactionType.CREDIT:
        return 'Dépôt';
      case TransactionType.DEBIT:
        return 'Débit';
      case TransactionType.REFUND:
        return 'Remboursement';
      default:
        return type;
    }
  }

  /**
   * Obtient l'icône du type de transaction
   */
  getTransactionIcon(type: TransactionType): string {
    switch (type) {
      case TransactionType.CREDIT:
        return 'arrow-down';
      case TransactionType.DEBIT:
        return 'arrow-up';
      case TransactionType.REFUND:
        return 'rotate-left';
      default:
        return 'circle';
    }
  }
}

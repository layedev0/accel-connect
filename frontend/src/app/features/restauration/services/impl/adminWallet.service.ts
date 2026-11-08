import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import { UserBalanceStatsDTO, DepositRequest } from '../../model/balance.model';
import { UpdateBalanceRequest, AddDebtRequest } from '../../model/adminWallet.model';

export interface UserWalletStats extends UserBalanceStatsDTO {
  totalOrders: number;
}

@Injectable({
  providedIn: 'root',
})
export class AdminWalletService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/balance`;

  /**
   * 📊 Récupère les statistiques de tous les utilisateurs
   */
  getAllUserStats(): Observable<UserWalletStats[]> {
    return this.http.get<UserWalletStats[]>(`${this.apiUrl}/admin/stats`);
  }

  /**
   * 💵 Effectue un dépôt pour un utilisateur
   */
  depositToUser(request: DepositRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/deposit`, request);
  }

  /**
   * 💸 Ajoute une dette à un utilisateur
   */
  addDebt(request: AddDebtRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/add-debt`, request);
  }

  /**
   * 🔄 Met à jour le solde d'un utilisateur
   */
  updateBalance(request: UpdateBalanceRequest): Observable<any> {
    return this.http.put(`${this.apiUrl}/update`, request);
  }

  /**
   * 💱 Formate un montant en FCFA
   */
  formatAmount(amount: number): string {
    return (
      new Intl.NumberFormat('fr-FR', {
        minimumFractionDigits: 0,
        maximumFractionDigits: 0,
      }).format(amount) + ' FCFA'
    );
  }

  /**
   * 🎨 Obtient la couleur du statut
   */
  getStatusColor(status: string): string {
    if (status.includes('Dette')) {
      return 'bg-red-100 text-red-700 border-red-300';
    }
    if (status.includes('rembourser')) {
      return 'bg-green-100 text-green-700 border-green-300';
    }
    return 'bg-blue-100 text-blue-700 border-blue-300';
  }
}

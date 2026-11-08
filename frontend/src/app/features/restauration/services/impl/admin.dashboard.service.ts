import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, forkJoin } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../../../environments/environment';
import { DashboardView, RestaurantStats, UserOrderSummary } from '../../model/adminOrder.model';
import { UserBalanceStatsDTO } from '../../model/balance.model';
import { OrderDTO } from '../../model/order.model';

@Injectable({
  providedIn: 'root',
})
export class AdminDashboardService {
  private readonly http = inject(HttpClient);
  private readonly ordersUrl = `${environment.apiUrl}/admin/orders`;
  private readonly balanceUrl = `${environment.apiUrl}/balance`;

  /**
   * 📊 Charge toutes les données pour une date
   */
  loadDashboardData(date: string): Observable<DashboardView> {
    const params = new HttpParams().set('date', date);

    return forkJoin({
      orders: this.http.get<OrderDTO[]>(`${this.ordersUrl}/by-date`, { params }),
      userStats: this.http.get<UserBalanceStatsDTO[]>(`${this.balanceUrl}/admin/stats`),
    }).pipe(
      map(({ orders, userStats }) => {
        return this.processDashboardData(date, orders, userStats);
      })
    );
  }

  /**
   * 🔄 Traite les données pour le dashboard
   */
  private processDashboardData(
    date: string,
    orders: OrderDTO[],
    userStats: UserBalanceStatsDTO[]
  ): DashboardView {
    // Statistiques par restaurant
    const restaurantMap = new Map<number, RestaurantStats>();

    orders.forEach((order) => {
      order.items.forEach((item) => {
        const existing = restaurantMap.get(item.restaurantId) || {
          restaurantId: item.restaurantId,
          restaurantName: item.restaurantName,
          totalOrders: 0,
          totalAmount: 0,
          confirmedCount: 0,
          cancelledCount: 0,
        };

        existing.totalOrders++;
        if (order.status === 'CONFIRMED') {
          existing.totalAmount += item.subtotal;
          existing.confirmedCount++;
        } else {
          existing.cancelledCount++;
        }

        restaurantMap.set(item.restaurantId, existing);
      });
    });

    const restaurantStats = Array.from(restaurantMap.values()).sort(
      (a, b) => b.totalAmount - a.totalAmount
    );

    // Commandes par utilisateur
    const userMap = new Map<number, UserOrderSummary>();

    orders.forEach((order) => {
      const existing = userMap.get(order.userId) || {
        userId: order.userId,
        username: order.username,
        email: '',
        orders: [],
        totalAmount: 0,
        restaurants: [],
        meals: [],
        balance: 0,
        hasDebt: false,
      };

      existing.orders.push(order);

      if (order.status === 'CONFIRMED') {
        existing.totalAmount += order.totalAmount;
      }

      // Collecter restaurants et plats
      order.items.forEach((item) => {
        if (!existing.restaurants.includes(item.restaurantName)) {
          existing.restaurants.push(item.restaurantName);
        }

        const mealIndex = existing.meals.findIndex(
          (m) => m.name === item.mealName && m.restaurant === item.restaurantName
        );

        if (mealIndex >= 0) {
          existing.meals[mealIndex].quantity += item.quantity;
        } else {
          existing.meals.push({
            name: item.mealName,
            quantity: item.quantity,
            restaurant: item.restaurantName,
          });
        }
      });

      userMap.set(order.userId, existing);
    });

    // Enrichir avec les infos de balance
    userStats.forEach((stat) => {
      const user = userMap.get(stat.userId);
      if (user) {
        user.email = stat.email;
        user.balance = stat.balance;
        user.hasDebt = stat.hasDebt;
      }
    });

    const userOrders = Array.from(userMap.values()).sort((a, b) => b.totalAmount - a.totalAmount);

    return {
      date,
      restaurantStats,
      userOrders,
      globalStats: {
        totalOrders: orders.length,
        totalAmount: orders
          .filter((o) => o.status === 'CONFIRMED')
          .reduce((sum, o) => sum + o.totalAmount, 0),
        totalUsers: userOrders.length,
        totalRestaurants: restaurantStats.length,
      },
    };
  }

  /**
   * 👥 Récupère tous les utilisateurs (pour le sélecteur admin)
   */
  getAllUsers(): Observable<UserBalanceStatsDTO[]> {
    return this.http.get<UserBalanceStatsDTO[]>(`${this.balanceUrl}/admin/stats`);
  }

  /**
   * 💰 Effectue un dépôt
   */
  deposit(userId: number, amount: number): Observable<any> {
    return this.http.post(`${this.balanceUrl}/deposit`, {
      userId,
      amount,
      description: `Dépôt administrateur`,
    });
  }

  /**
   * 📥 Exporte en CSV pour un restaurant
   */
  exportRestaurantCSV(restaurantName: string, orders: OrderDTO[], date: string): void {
    const headers = ['Utilisateur', 'Plats', 'Quantité', 'Montant Total'];

    const rows = orders.map((order) => {
      const items = order.items.map((item) => `${item.mealName} (×${item.quantity})`).join(', ');
      return [
        order.username,
        items,
        order.items.reduce((sum, i) => sum + i.quantity, 0),
        order.totalAmount,
      ];
    });

    const csv = [headers.join(','), ...rows.map((row) => row.join(','))].join('\n');

    const BOM = '\uFEFF';
    const blob = new Blob([BOM + csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `${restaurantName}_${date}.csv`;
    link.click();
  }

  /**
   * 💱 Formate un montant
   */
  formatAmount(amount: number): string {
    return new Intl.NumberFormat('fr-FR', {
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(amount);
  }
}

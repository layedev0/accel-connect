import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import { OrderDTO } from '../../model/order.model';

export interface AdminActionResponse {
  success: boolean;
  message: string;
  order: OrderDTO;
  refundedAmount?: number;
  chargedAmount?: number;
}

export interface AdminOrderItemData {
  mealId: number;
  restaurantId: number;
  quantity: number;
  unitPrice: number;
}

export interface AdminCreateOrderRequest {
  userId: number;
  date: string;
  items: AdminOrderItemData[];
}

@Injectable({
  providedIn: 'root',
})
export class AdminOrderService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/admin/orders`;

  /**
   * ❌ Annule une commande (admin - sans contrainte horaire)
   */
  adminCancelOrder(orderId: number, reason?: string): Observable<AdminActionResponse> {
    const url = `${this.apiUrl}/${orderId}/admin-cancel${
      reason ? `?reason=${encodeURIComponent(reason)}` : ''
    }`;
    return this.http.put<AdminActionResponse>(url, {});
  }

  /**
   * 🗑️ Supprime un plat d'une commande
   */
  removeOrderItem(
    orderId: number,
    orderItemId: number,
    reason?: string
  ): Observable<AdminActionResponse> {
    const url = `${this.apiUrl}/${orderId}/items/${orderItemId}${
      reason ? `?reason=${encodeURIComponent(reason)}` : ''
    }`;
    return this.http.delete<AdminActionResponse>(url);
  }

  /**
   * ✏️ Modifie la quantité d'un plat
   */
  updateOrderItemQuantity(
    orderId: number,
    orderItemId: number,
    newQuantity: number,
    reason?: string
  ): Observable<AdminActionResponse> {
    const url = `${this.apiUrl}/${orderId}/items/${orderItemId}/quantity`;
    return this.http.put<AdminActionResponse>(url, {
      newQuantity,
      reason,
    });
  }

  /**
   * 🔄 Réactive une commande annulée
   */
  reactivateOrder(orderId: number, reason?: string): Observable<AdminActionResponse> {
    const url = `${this.apiUrl}/${orderId}/reactivate${
      reason ? `?reason=${encodeURIComponent(reason)}` : ''
    }`;
    return this.http.put<AdminActionResponse>(url, {});
  }

  /**
   * ➕ Crée ou remplace une commande pour un utilisateur (sans contrainte horaire)
   */
  adminCreateOrder(request: AdminCreateOrderRequest): Observable<AdminActionResponse> {
    return this.http.post<AdminActionResponse>(`${this.apiUrl}/for-user`, request);
  }
}

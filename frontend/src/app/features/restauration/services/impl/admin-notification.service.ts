import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';

export interface NotificationResult {
  success: boolean;
  sent: number;
  failed: number;
  total: number;
  restaurantName?: string;
}

@Injectable({
  providedIn: 'root',
})
export class AdminNotificationService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/admin/notify`;

  notifyMenuAvailable(): Observable<NotificationResult> {
    return this.http.post<NotificationResult>(`${this.apiUrl}/menu-available`, {});
  }

  notifyRestaurantDelivered(restaurantId: number, date?: string): Observable<NotificationResult> {
    const url = date
      ? `${this.apiUrl}/restaurant-delivered/${restaurantId}?date=${date}`
      : `${this.apiUrl}/restaurant-delivered/${restaurantId}`;
    return this.http.post<NotificationResult>(url, {});
  }
}

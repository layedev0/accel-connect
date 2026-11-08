import { Injectable, inject, signal, computed, OnDestroy } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

export interface AppNotification {
  id: number;
  title: string;
  message: string;
  link: string;
  isRead: boolean;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService implements OnDestroy {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/notifications`;

  notifications = signal<AppNotification[]>([]);
  unreadCount = computed(() => this.notifications().filter(n => !n.isRead).length);

  private intervalId: ReturnType<typeof setInterval> | null = null;

  startPolling(): void {
    this.load();
    this.intervalId = setInterval(() => this.load(), 30_000);
  }

  stopPolling(): void {
    if (this.intervalId) {
      clearInterval(this.intervalId);
      this.intervalId = null;
    }
  }

  ngOnDestroy(): void {
    this.stopPolling();
  }

  markRead(id: number): void {
    this.http.patch<void>(`${this.baseUrl}/${id}/read`, {}).subscribe(() => {
      this.notifications.update(list =>
        list.map(n => n.id === id ? { ...n, isRead: true } : n)
      );
    });
  }

  markAllRead(): void {
    this.http.patch<void>(`${this.baseUrl}/mark-all-read`, {}).subscribe(() => {
      this.notifications.update(list => list.map(n => ({ ...n, isRead: true })));
    });
  }

  private load(): void {
    this.http.get<AppNotification[]>(this.baseUrl).subscribe({
      next: list => this.notifications.set(list),
      error: () => {}
    });
  }
}

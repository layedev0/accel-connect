import { Injectable, signal } from '@angular/core';
import { Notification } from '../../models/notification.model';

@Injectable({
  providedIn: 'root',
})
export class NotificationService {
  notifications = signal<Notification[]>([]);

  /**
   * Affiche une notification de succès
   */
  success(title: string, message: string, duration: number = 5000): void {
    this.show({
      id: this.generateId(),
      type: 'success',
      title,
      message,
      duration,
    });
  }

  /**
   * Affiche une notification d'erreur
   */
  error(title: string, message: string, duration: number = 7000): void {
    this.show({
      id: this.generateId(),
      type: 'error',
      title,
      message,
      duration,
    });
  }

  /**
   * Affiche une notification d'avertissement
   */
  warning(title: string, message: string, duration: number = 6000): void {
    this.show({
      id: this.generateId(),
      type: 'warning',
      title,
      message,
      duration,
    });
  }

  /**
   * Affiche une notification d'information
   */
  info(title: string, message: string, duration: number = 5000): void {
    this.show({
      id: this.generateId(),
      type: 'info',
      title,
      message,
      duration,
    });
  }

  /**
   * Affiche une notification avec action
   */
  showWithAction(
    type: Notification['type'],
    title: string,
    message: string,
    actionLabel: string,
    actionCallback: () => void,
    duration: number = 8000
  ): void {
    this.show({
      id: this.generateId(),
      type,
      title,
      message,
      duration,
      action: {
        label: actionLabel,
        callback: actionCallback,
      },
    });
  }

  /**
   * Affiche une notification
   */
  private show(notification: Notification): void {
    const current = this.notifications();
    this.notifications.set([...current, notification]);

    // Auto-suppression après la durée spécifiée
    if (notification.duration && notification.duration > 0) {
      setTimeout(() => {
        this.remove(notification.id);
      }, notification.duration);
    }
  }

  /**
   * Supprime une notification
   */
  remove(id: string): void {
    const current = this.notifications();
    this.notifications.set(current.filter((n) => n.id !== id));
  }

  /**
   * Supprime toutes les notifications
   */
  clear(): void {
    this.notifications.set([]);
  }

  /**
   * Génère un ID unique
   */
  private generateId(): string {
    return `notification-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  }
}

import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { Notification } from '../../../core/models/notification.model';
import { NotificationService } from '../../../core/services/impl/notification.service';

@Component({
  selector: 'app-notification',
  standalone: true,
  imports: [CommonModule, FontAwesomeModule],
  template: `
    <div class="fixed top-4 right-4 z-[9999] space-y-3 max-w-md w-full pointer-events-none">
      @for (notification of notificationService.notifications(); track notification.id) {
      <div
        class="pointer-events-auto transform transition-all duration-300 ease-out animate-slide-in-right"
        [class.animate-slide-out-right]="isRemoving(notification.id)"
      >
        <div
          class="bg-white rounded-2xl shadow-2xl border-2 overflow-hidden"
          [class.border-green-500]="notification.type === 'success'"
          [class.border-red-500]="notification.type === 'error'"
          [class.border-yellow-500]="notification.type === 'warning'"
          [class.border-blue-500]="notification.type === 'info'"
        >
          <!-- Progress Bar -->
          @if (notification.duration) {
          <div
            class="h-1 transition-all ease-linear"
            [class.bg-green-500]="notification.type === 'success'"
            [class.bg-red-500]="notification.type === 'error'"
            [class.bg-yellow-500]="notification.type === 'warning'"
            [class.bg-blue-500]="notification.type === 'info'"
            [style.animation]="'progress ' + notification.duration + 'ms linear'"
          ></div>
          }

          <div class="p-4">
            <div class="flex items-start gap-3">
              <!-- Icon -->
              <div
                class="w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0 shadow-lg"
                [class.bg-gradient-to-br]="true"
                [class.from-green-500]="notification.type === 'success'"
                [class.to-green-600]="notification.type === 'success'"
                [class.from-red-500]="notification.type === 'error'"
                [class.to-red-600]="notification.type === 'error'"
                [class.from-yellow-500]="notification.type === 'warning'"
                [class.to-yellow-600]="notification.type === 'warning'"
                [class.from-blue-500]="notification.type === 'info'"
                [class.to-blue-600]="notification.type === 'info'"
              >
                <fa-icon
                  [icon]="['fas', getIcon(notification.type)]"
                  class="text-white text-lg"
                ></fa-icon>
              </div>

              <!-- Content -->
              <div class="flex-1 min-w-0">
                <h4 class="text-base font-bold text-gray-900 mb-1">
                  {{ notification.title }}
                </h4>
                <p class="text-sm text-gray-600 leading-relaxed">
                  {{ notification.message }}
                </p>

                <!-- Action Button -->
                @if (notification.action) {
                <button
                  (click)="handleAction(notification)"
                  class="mt-3 px-4 py-2 rounded-lg text-sm font-bold transition-all hover:scale-105"
                  [class.bg-green-500]="notification.type === 'success'"
                  [class.hover:bg-green-600]="notification.type === 'success'"
                  [class.bg-red-500]="notification.type === 'error'"
                  [class.hover:bg-red-600]="notification.type === 'error'"
                  [class.bg-yellow-500]="notification.type === 'warning'"
                  [class.hover:bg-yellow-600]="notification.type === 'warning'"
                  [class.bg-blue-500]="notification.type === 'info'"
                  [class.hover:bg-blue-600]="notification.type === 'info'"
                  class="text-white shadow-md"
                >
                  {{ notification.action.label }}
                </button>
                }
              </div>

              <!-- Close Button -->
              <button
                (click)="close(notification.id)"
                class="w-8 h-8 rounded-lg hover:bg-gray-100 flex items-center justify-center transition-all flex-shrink-0"
              >
                <fa-icon
                  [icon]="['fas', 'times']"
                  class="text-gray-400 hover:text-gray-600"
                ></fa-icon>
              </button>
            </div>
          </div>
        </div>
      </div>
      }
    </div>
  `,
  styles: [
    `
      @keyframes slide-in-right {
        from {
          transform: translateX(100%);
          opacity: 0;
        }
        to {
          transform: translateX(0);
          opacity: 1;
        }
      }

      @keyframes slide-out-right {
        from {
          transform: translateX(0);
          opacity: 1;
        }
        to {
          transform: translateX(100%);
          opacity: 0;
        }
      }

      @keyframes progress {
        from {
          width: 100%;
        }
        to {
          width: 0%;
        }
      }

      .animate-slide-in-right {
        animation: slide-in-right 0.3s ease-out;
      }

      .animate-slide-out-right {
        animation: slide-out-right 0.3s ease-in;
      }
    `,
  ],
})
export class NotificationComponent {
  notificationService = inject(NotificationService);
  private readonly removingIds = new Set<string>();

  getIcon(type: Notification['type']): string {
    const icons = {
      success: 'check-circle',
      error: 'exclamation-circle',
      warning: 'exclamation-triangle',
      info: 'info-circle',
    };
    return icons[type];
  }

  close(id: string): void {
    this.removingIds.add(id);
    setTimeout(() => {
      this.notificationService.remove(id);
      this.removingIds.delete(id);
    }, 300);
  }

  isRemoving(id: string): boolean {
    return this.removingIds.has(id);
  }

  handleAction(notification: Notification): void {
    if (notification.action) {
      notification.action.callback();
      this.close(notification.id);
    }
  }
}

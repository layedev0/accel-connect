import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { ConfirmationService } from '../../../core/services/impl/ConfirmationDialog.service';

@Component({
  selector: 'app-confirmation',
  standalone: true,
  imports: [CommonModule, FontAwesomeModule],
  template: `
    @if (confirmationService.isOpen()) {
    <!-- Backdrop -->
    <div
      class="fixed inset-0 bg-black/50 backdrop-blur-sm z-[9998] transition-opacity duration-300"
      (click)="confirmationService.handleCancel()"
    ></div>

    <!-- Modal -->
    <div class="fixed inset-0 z-[9999] flex items-center justify-center p-4">
      <div
        class="bg-white rounded-3xl shadow-2xl max-w-md w-full transform transition-all duration-300 animate-scale-in"
        (click)="$event.stopPropagation()"
      >
        @if (confirmationService.currentDialog(); as dialog) {
        <!-- Header avec icône -->
        <div class="p-6 pb-4">
          <div class="flex items-start gap-4">
            <!-- Icon -->
            <div
              class="w-14 h-14 rounded-2xl flex items-center justify-center flex-shrink-0 shadow-lg"
              [class.bg-gradient-to-br]="true"
              [class.from-red-500]="dialog.type === 'danger'"
              [class.to-red-600]="dialog.type === 'danger'"
              [class.from-yellow-500]="dialog.type === 'warning'"
              [class.to-yellow-600]="dialog.type === 'warning'"
              [class.from-blue-500]="dialog.type === 'info'"
              [class.to-blue-600]="dialog.type === 'info'"
              [class.from-green-500]="dialog.type === 'success'"
              [class.to-green-600]="dialog.type === 'success'"
            >
              <fa-icon [icon]="['fas', getIcon(dialog.type)]" class="text-white text-2xl"></fa-icon>
            </div>

            <!-- Content -->
            <div class="flex-1">
              <h3 class="text-2xl font-black text-gray-900 mb-2">
                {{ dialog.title }}
              </h3>
              <p class="text-gray-600 leading-relaxed">
                {{ dialog.message }}
              </p>
            </div>
          </div>
        </div>

        <!-- Actions -->
        <div class="p-6 pt-4 flex items-center gap-3">
          <button
            (click)="confirmationService.handleCancel()"
            class="flex-1 px-6 py-3 rounded-xl font-bold text-gray-700 bg-gray-100 hover:bg-gray-200 transition-all hover:scale-105"
          >
            {{ dialog.cancelText }}
          </button>
          <button
            (click)="confirmationService.handleConfirm()"
            class="flex-1 px-6 py-3 rounded-xl font-bold text-white transition-all hover:scale-105 shadow-lg"
            [class.bg-gradient-to-r]="true"
            [class.from-red-500]="dialog.type === 'danger'"
            [class.to-red-600]="dialog.type === 'danger'"
            [class.hover:from-red-600]="dialog.type === 'danger'"
            [class.hover:to-red-700]="dialog.type === 'danger'"
            [class.from-yellow-500]="dialog.type === 'warning'"
            [class.to-yellow-600]="dialog.type === 'warning'"
            [class.hover:from-yellow-600]="dialog.type === 'warning'"
            [class.hover:to-yellow-700]="dialog.type === 'warning'"
            [class.from-blue-500]="dialog.type === 'info'"
            [class.to-blue-600]="dialog.type === 'info'"
            [class.hover:from-blue-600]="dialog.type === 'info'"
            [class.hover:to-blue-700]="dialog.type === 'info'"
            [class.from-green-500]="dialog.type === 'success'"
            [class.to-green-600]="dialog.type === 'success'"
            [class.hover:from-green-600]="dialog.type === 'success'"
            [class.hover:to-green-700]="dialog.type === 'success'"
          >
            {{ dialog.confirmText }}
          </button>
        </div>
        }
      </div>
    </div>
    }
  `,
  styles: [
    `
      @keyframes scale-in {
        from {
          transform: scale(0.9);
          opacity: 0;
        }
        to {
          transform: scale(1);
          opacity: 1;
        }
      }

      .animate-scale-in {
        animation: scale-in 0.3s ease-out;
      }
    `,
  ],
})
export class ConfirmationComponent {
  confirmationService = inject(ConfirmationService);

  getIcon(type?: string): string {
    const icons = {
      danger: 'exclamation-circle',
      warning: 'exclamation-triangle',
      info: 'info-circle',
      success: 'check-circle',
    };
    return icons[type as keyof typeof icons] || 'info-circle';
  }
}

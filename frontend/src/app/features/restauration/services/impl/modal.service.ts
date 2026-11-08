import { computed, Injectable, signal } from '@angular/core';

export interface ModalState {
  weeklyExport: boolean;
  payment: boolean;
  stats: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class ModalService {
  private readonly state = signal<ModalState>({
    weeklyExport: false,
    payment: false,
    stats: false,
  });

  // Getters
  showWeeklyExportModal = computed(() => this.state().weeklyExport);
  showPaymentModal = computed(() => this.state().payment);
  showStatsModal = computed(() => this.state().stats);

  // Actions
  openWeeklyExportModal(): void {
    this.state.update(s => ({ ...s, weeklyExport: true }));
  }

  closeWeeklyExportModal(): void {
    this.state.update(s => ({ ...s, weeklyExport: false }));
  }

  openPaymentModal(): void {
    this.state.update(s => ({ ...s, payment: true }));
  }

  closePaymentModal(): void {
    this.state.update(s => ({ ...s, payment: false }));
  }

  openStatsModal(): void {
    this.state.update(s => ({ ...s, stats: true }));
  }

  closeStatsModal(): void {
    this.state.update(s => ({ ...s, stats: false }));
  }

  closeAll(): void {
    this.state.set({
      weeklyExport: false,
      payment: false,
      stats: false,
    });
  }
}

import { Component, OnInit, OnDestroy, signal, computed, inject, Output, EventEmitter, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { Subject, takeUntil } from 'rxjs';
import { ExportHistory, WeekInfo, WeeklyExportService } from '../../services/impl/weekly-export.service';
import { NotificationService } from '../../../../core/services/impl/notification.service';
import { ModalService } from '../../services/impl/modal.service';

@Component({
  selector: 'app-weekly-export-modal',
  standalone: true,
  imports: [CommonModule, FontAwesomeModule],
  templateUrl: './weekly-export-modal.html',
  styleUrls: ['./weekly-export-modal.css'],
})
export class WeeklyExportModal implements OnInit, OnDestroy {
  private readonly weeklyExportService = inject(WeeklyExportService);
  private readonly notificationService = inject(NotificationService);
  private readonly modalService = inject(ModalService);
  private readonly destroy$ = new Subject<void>();

  activeTab = signal<'weeks' | 'history' | 'consumption'>('weeks');
  isLoading = signal(true);
  isDownloading = signal(false);
  downloadingWeek = signal<string | null>(null);
  redownloadingId = signal<number | null>(null);

  // Pour l'onglet consommation
  consumptionPeriodType = signal<'all' | 'week' | 'day'>('all');
  consumptionStartDate = signal<string>('');
  consumptionEndDate = signal<string>('');

  availableWeeks = signal<WeekInfo[]>([]);
  exportHistory = signal<ExportHistory[]>([]);
  @Output() close = new EventEmitter<void>();
  @HostListener('document:keydown.escape')
  onEscapeKey(): void {
    this.closeModal();
  }

  sortedWeeks = computed(() => {
    return [...this.availableWeeks()].sort((a, b) => {
      return new Date(b.startDate).getTime() - new Date(a.startDate).getTime();
    });
  });

  sortedHistory = computed(() => {
    return [...this.exportHistory()].sort((a, b) => {
      return new Date(b.exportDate).getTime() - new Date(a.exportDate).getTime();
    });
  });

  ngOnInit(): void {
    this.loadData();
    document.body.style.overflow = 'hidden';
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    document.body.style.overflow = 'auto';
  }

  loadData(): void {
    this.isLoading.set(true);

    this.weeklyExportService.getAvailableWeeks()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (weeks) => {
          this.availableWeeks.set(weeks);
          this.loadHistory();
        },
        error: (error) => {
          this.isLoading.set(false);
          this.notificationService.error(
            '❌ Erreur',
            'Impossible de charger les semaines disponibles'
          );
        },
      });
  }

  loadHistory(): void {
    this.weeklyExportService.getExportHistory()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (history) => {
          this.exportHistory.set(history);
          this.isLoading.set(false);
        },
        error: (error) => {
          this.isLoading.set(false);
          this.notificationService.error(
            '❌ Erreur',
            "Impossible de charger l'historique"
          );
        },
      });
  }

  downloadWeek(startDate: string, endDate: string): void {
    this.isDownloading.set(true);
    this.downloadingWeek.set(startDate);

    this.weeklyExportService.downloadWeeklyReport(startDate, endDate)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.isDownloading.set(false);
          this.downloadingWeek.set(null);
          this.loadData(); // Refresh data
        },
        error: (error) => {
          this.isDownloading.set(false);
          this.downloadingWeek.set(null);
          this.notificationService.error(
            '❌ Erreur',
            error.error?.message || 'Impossible de télécharger le rapport'
          );
        },
      });
  }

  redownload(historyId: number): void {
    this.isDownloading.set(true);
    this.redownloadingId.set(historyId);

    this.weeklyExportService.redownloadFromHistory(historyId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.isDownloading.set(false);
          this.redownloadingId.set(null);
        },
        error: (error) => {
          this.isDownloading.set(false);
          this.redownloadingId.set(null);
          this.notificationService.error(
            '❌ Erreur',
            'Impossible de re-télécharger le rapport'
          );
        },
      });
  }

  closeModal(): void {
    this.modalService.closeWeeklyExportModal();
    this.close.emit();
  }

  formatDate(date: string): string {
    return new Date(date).toLocaleDateString('fr-FR', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
    });
  }

  formatDateTime(date: string): string {
    return new Date(date).toLocaleDateString('fr-FR', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  formatAmount(amount: number): string {
    return this.weeklyExportService.formatAmount(amount);
  }

  formatFileSize(bytes: number): string {
    return this.weeklyExportService.formatFileSize(bytes);
  }

  downloadConsumptionReport(): void {
    this.isDownloading.set(true);

    const periodType = this.consumptionPeriodType();
    const startDate = this.consumptionStartDate() || undefined;
    const endDate = this.consumptionEndDate() || undefined;

    this.weeklyExportService.downloadConsumptionReport(periodType, startDate, endDate)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.isDownloading.set(false);
        },
        error: (error) => {
          this.isDownloading.set(false);
          this.notificationService.error(
            '❌ Erreur',
            'Impossible de télécharger le rapport de consommation'
          );
        },
      });
  }

  getTodayDate(): string {
    const today = new Date();
    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, '0');
    const day = String(today.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  getCurrentWeekStart(): string {
    const today = new Date();
    const dayOfWeek = today.getDay();
    const diff = today.getDate() - dayOfWeek + (dayOfWeek === 0 ? -6 : 1);
    const monday = new Date(today.setDate(diff));
    return this.formatDateToISO(monday);
  }

  getCurrentWeekEnd(): string {
    const today = new Date();
    const dayOfWeek = today.getDay();
    const diff = today.getDate() - dayOfWeek + (dayOfWeek === 0 ? -6 : 1);
    const monday = new Date(today.setDate(diff));
    const friday = new Date(monday.getTime() + 4 * 24 * 60 * 60 * 1000);
    return this.formatDateToISO(friday);
  }

  private formatDateToISO(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}

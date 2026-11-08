import { Component, OnInit, inject, signal } from '@angular/core';
import { retry } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { AbsenceService, AbsenceSummary } from '../services/absence.service';
import { UserService } from '../../auth/service/user.service';
import { KeycloakService } from '../../auth/service/keycloak.service';

@Component({
  selector: 'app-absence-approval',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './absence-approval-component.html',
})
export class AbsenceApprovalComponent implements OnInit {
  private readonly absenceService = inject(AbsenceService);
  private readonly userService = inject(UserService);
  private readonly keycloakService = inject(KeycloakService);

  readonly currentUserId = signal<number | null>(null);

  readonly requests = signal<AbsenceSummary[]>([]);
  readonly approvedRequests = signal<AbsenceSummary[]>([]);
  readonly rejectedRequests = signal<AbsenceSummary[]>([]);
  readonly activeTab = signal<'pending' | 'approved' | 'rejected'>('pending');
  readonly loading = signal(true);
  readonly errorMsg = signal<string | null>(null);
  readonly successMsg = signal<string | null>(null);

  readonly modalRequest = signal<AbsenceSummary | null>(null);
  readonly modalAction = signal<'approve' | 'reject' | 'cancel' | 'cancelRejection' | null>(null);
  readonly modalComment = signal('');
  readonly modalSubmitting = signal(false);

  readonly expandedId = signal<number | null>(null);

  // Approved / rejected filters (shared by both history tabs)
  readonly filterEmail = signal('');
  readonly filterFirstName = signal('');
  readonly filterStartDate = signal('');
  readonly filterEndDate = signal('');

  /** Requests of the active history tab (approved or rejected). */
  historyRequests(): AbsenceSummary[] {
    return this.activeTab() === 'rejected' ? this.rejectedRequests() : this.approvedRequests();
  }

  filteredApprovedRequests(): AbsenceSummary[] {
    const email = this.filterEmail().trim().toLowerCase();
    const firstName = this.filterFirstName().trim().toLowerCase();
    const start = this.filterStartDate();
    const end = this.filterEndDate();
    return this.historyRequests().filter(r => {
      if (email && !(r.employeeEmail ?? '').toLowerCase().includes(email)) return false;
      if (firstName && !(r.employeeName ?? '').toLowerCase().includes(firstName)) return false;
      if (start && r.date < start) return false;
      if (end && r.date > end) return false;
      return true;
    });
  }

  resetApprovedFilters(): void {
    this.filterEmail.set('');
    this.filterFirstName.set('');
    this.filterStartDate.set('');
    this.filterEndDate.set('');
    this.approvedPage.set(1);
  }

  readonly pageSize = 10;
  readonly pendingPage = signal(1);
  readonly approvedPage = signal(1);

  pendingPaged(): AbsenceSummary[] {
    const start = (this.pendingPage() - 1) * this.pageSize;
    return this.requests().slice(start, start + this.pageSize);
  }

  approvedPaged(): AbsenceSummary[] {
    const filtered = this.filteredApprovedRequests();
    const start = (this.approvedPage() - 1) * this.pageSize;
    return filtered.slice(start, start + this.pageSize);
  }

  pendingTotalPages(): number {
    return Math.ceil(this.requests().length / this.pageSize);
  }

  approvedTotalPages(): number {
    return Math.ceil(this.filteredApprovedRequests().length / this.pageSize);
  }

  pageRange(total: number): number[] {
    return Array.from({ length: total }, (_, i) => i + 1);
  }

  min(a: number, b: number): number {
    return Math.min(a, b);
  }

  ngOnInit(): void {
    this.load();
    this.userService.getCurrentUser().subscribe({
      next: (u) => this.currentUserId.set(u.id),
      error: () => {},
    });
  }

  load(): void {
    this.loading.set(true);
    this.errorMsg.set(null);
    this.pendingPage.set(1);
    this.approvedPage.set(1);
    this.absenceService.getPendingAbsences().pipe(
      retry({ count: 2, delay: 1000 })
    ).subscribe({
      next: (reqs) => { this.requests.set(reqs); this.loading.set(false); },
      error: () => { this.loading.set(false); this.errorMsg.set('loadFailed'); },
    });
    this.absenceService.getApprovedAbsences().pipe(
      retry({ count: 1, delay: 1000 })
    ).subscribe({
      next: (reqs) => { this.approvedRequests.set(reqs); this.approvedPage.set(1); },
      error: () => {},
    });
    this.absenceService.getRejectedAbsences().pipe(
      retry({ count: 1, delay: 1000 })
    ).subscribe({
      next: (reqs) => { this.rejectedRequests.set(reqs); this.approvedPage.set(1); },
      error: () => {},
    });
  }

  setTab(tab: 'pending' | 'approved' | 'rejected'): void {
    this.activeTab.set(tab);
    this.approvedPage.set(1);
  }

  toggleExpand(id: number): void {
    this.expandedId.update((cur) => (cur === id ? null : id));
  }

  openModal(req: AbsenceSummary, action: 'approve' | 'reject' | 'cancel' | 'cancelRejection'): void {
    this.modalRequest.set(req);
    this.modalAction.set(action);
    this.modalComment.set('');
  }

  closeModal(): void {
    this.modalRequest.set(null);
    this.modalAction.set(null);
    this.modalComment.set('');
  }

  confirmAction(): void {
    const req = this.modalRequest();
    const action = this.modalAction();
    if (!req || !action) return;

    this.modalSubmitting.set(true);
    const comment = this.modalComment().trim() || undefined;

    const op$ = action === 'approve'
      ? this.absenceService.approveAbsence(req.id, comment)
      : action === 'reject'
        ? this.absenceService.rejectAbsence(req.id, comment)
        : action === 'cancel'
          ? this.absenceService.cancelApprovedAbsence(req.id, comment)
          : this.absenceService.cancelRejectedAbsence(req.id, comment);

    const successMessages: Record<string, string> = {
      approve: `Absence de ${req.employeeName ?? "l'employé"} approuvée.`,
      reject:  `Absence de ${req.employeeName ?? "l'employé"} rejetée.`,
      cancel:  `Absence de ${req.employeeName ?? "l'employé"} annulée.`,
      cancelRejection: `Rejet annulé : l'absence de ${req.employeeName ?? "l'employé"} est de nouveau en attente.`,
    };

    op$.subscribe({
      next: () => {
        this.successMsg.set(successMessages[action]);
        this.modalSubmitting.set(false);
        this.closeModal();
        this.load();
        setTimeout(() => this.successMsg.set(null), 4000);
      },
      error: (err) => {
        this.errorMsg.set(err?.error?.message ?? 'Une erreur est survenue.');
        this.modalSubmitting.set(false);
        this.closeModal();
      },
    });
  }

  canCancel(req: AbsenceSummary): boolean {
    if (this.keycloakService.hasAnyRole(['HR_MANAGER', 'ROLE_HR_MANAGER'])) return true;
    const uid = this.currentUserId();
    if (uid === null) return false;
    return (req.approvers ?? []).some(a => a.id === uid);
  }

  statusClass(status: string): string {
    switch (status) {
      case 'En attente': return 'bg-amber-100 text-amber-800';
      case 'Approuvé':   return 'bg-green-100 text-green-800';
      case 'Rejeté':     return 'bg-red-100 text-red-800';
      case 'Annulé':     return 'bg-gray-100 text-gray-500';
      default:           return 'bg-gray-100 text-gray-500';
    }
  }

  formatDate(d: string): string {
    if (!d) return '';
    return new Date(d).toLocaleDateString('fr-FR', {
      day: '2-digit', month: '2-digit', year: 'numeric',
    });
  }

  formatTime(t: string): string {
    if (!t) return '';
    return t.substring(0, 5);
  }
}

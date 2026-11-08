import { Component, OnInit, inject, signal } from '@angular/core';
import { AbsenceService, AbsenceSummary } from '../services/absence.service';
import { UserService } from '../../auth/service/user.service';
import { KeycloakService } from '../../auth/service/keycloak.service';

@Component({
  selector: 'app-approved-absences',
  standalone: true,
  imports: [],
  templateUrl: './approved-absences-component.html',
})
export class ApprovedAbsencesComponent implements OnInit {
  private readonly absenceService = inject(AbsenceService);
  private readonly userService = inject(UserService);
  private readonly keycloakService = inject(KeycloakService);

  readonly currentUserId = signal<number | null>(null);

  readonly allRequests = signal<AbsenceSummary[]>([]);
  readonly loading = signal(true);
  readonly errorMsg = signal<string | null>(null);
  readonly successMsg = signal<string | null>(null);

  // Filters
  readonly filterEmail = signal('');
  readonly filterFirstName = signal('');
  readonly filterStartDate = signal('');
  readonly filterEndDate = signal('');

  // Cancel modal
  readonly modalRequest = signal<AbsenceSummary | null>(null);
  readonly modalComment = signal('');
  readonly modalSubmitting = signal(false);

  // Pagination
  readonly pageSize = 15;
  readonly currentPage = signal(1);

  ngOnInit(): void {
    this.load();
    this.userService.getCurrentUser().subscribe({
      next: (u) => this.currentUserId.set(u.id),
      error: () => {},
    });
  }

  private load(): void {
    this.loading.set(true);
    this.currentPage.set(1);
    this.absenceService.getApprovedAbsences().subscribe({
      next: (reqs) => { this.allRequests.set(reqs); this.loading.set(false); },
      error: () => { this.errorMsg.set('Impossible de charger les absences approuvées.'); this.loading.set(false); },
    });
  }

  filteredRequests(): AbsenceSummary[] {
    const email = this.filterEmail().trim().toLowerCase();
    const firstName = this.filterFirstName().trim().toLowerCase();
    const start = this.filterStartDate();
    const end = this.filterEndDate();
    return this.allRequests().filter(r => {
      if (email && !(r.employeeEmail ?? '').toLowerCase().includes(email)) return false;
      if (firstName && !(r.employeeName ?? '').toLowerCase().includes(firstName)) return false;
      if (start && r.date < start) return false;
      if (end && r.date > end) return false;
      return true;
    });
  }

  paged(): AbsenceSummary[] {
    const start = (this.currentPage() - 1) * this.pageSize;
    return this.filteredRequests().slice(start, start + this.pageSize);
  }

  totalPages(): number {
    return Math.ceil(this.filteredRequests().length / this.pageSize);
  }

  pageRange(): number[] {
    return Array.from({ length: this.totalPages() }, (_, i) => i + 1);
  }

  min(a: number, b: number): number {
    return Math.min(a, b);
  }

  hasActiveFilters(): boolean {
    return !!(this.filterEmail() || this.filterFirstName() || this.filterStartDate() || this.filterEndDate());
  }

  resetFilters(): void {
    this.filterEmail.set('');
    this.filterFirstName.set('');
    this.filterStartDate.set('');
    this.filterEndDate.set('');
    this.currentPage.set(1);
  }

  openCancelModal(req: AbsenceSummary): void {
    this.modalRequest.set(req);
    this.modalComment.set('');
  }

  closeModal(): void {
    this.modalRequest.set(null);
    this.modalComment.set('');
  }

  confirmCancel(): void {
    const req = this.modalRequest();
    if (!req) return;
    this.modalSubmitting.set(true);
    const comment = this.modalComment().trim() || undefined;
    this.absenceService.cancelApprovedAbsence(req.id, comment).subscribe({
      next: () => {
        this.successMsg.set(`Absence de ${req.employeeName ?? "l'employé"} annulée.`);
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
      case 'Approuvé': return 'bg-green-100 text-green-800';
      case 'Rejeté':   return 'bg-red-100 text-red-800';
      case 'Annulé':   return 'bg-gray-100 text-gray-500';
      default:         return 'bg-gray-100 text-gray-500';
    }
  }

  formatDate(d: string): string {
    if (!d) return '';
    return new Date(d).toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric' });
  }

  formatTime(t: string): string {
    if (!t) return '';
    return t.substring(0, 5);
  }
}

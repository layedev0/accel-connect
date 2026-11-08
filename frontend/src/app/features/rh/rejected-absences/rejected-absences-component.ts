import { Component, OnInit, inject, signal } from '@angular/core';
import { retry } from 'rxjs';
import { AbsenceService, AbsenceSummary } from '../services/absence.service';

@Component({
  selector: 'app-rejected-absences',
  standalone: true,
  imports: [],
  templateUrl: './rejected-absences-component.html',
})
export class RejectedAbsencesComponent implements OnInit {
  private readonly absenceService = inject(AbsenceService);

  readonly allRequests = signal<AbsenceSummary[]>([]);
  readonly loading = signal(true);
  readonly errorMsg = signal<string | null>(null);

  // Filters
  readonly filterEmail = signal('');
  readonly filterFirstName = signal('');
  readonly filterStartDate = signal('');
  readonly filterEndDate = signal('');

  // Pagination
  readonly pageSize = 15;
  readonly currentPage = signal(1);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.errorMsg.set(null);
    this.currentPage.set(1);
    this.absenceService.getRejectedAbsences().pipe(
      retry({ count: 2, delay: 500 })
    ).subscribe({
      next: (reqs) => { this.allRequests.set(reqs); this.loading.set(false); },
      error: () => { this.loading.set(false); this.errorMsg.set('Impossible de charger les absences rejetées.'); },
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

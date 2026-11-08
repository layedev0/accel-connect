import { Component, OnInit, inject, signal } from '@angular/core';
import { retry } from 'rxjs';
import { LeaveService, LeaveRequestSummary } from '../services/leave.service';

@Component({
  selector: 'app-rejected-leaves',
  standalone: true,
  imports: [],
  templateUrl: './rejected-leaves-component.html',
})
export class RejectedLeavesComponent implements OnInit {
  private readonly leaveService = inject(LeaveService);

  readonly allRequests = signal<LeaveRequestSummary[]>([]);
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
    this.leaveService.getRejectedRequests().pipe(
      retry({ count: 2, delay: 500 })
    ).subscribe({
      next: (reqs) => { this.allRequests.set(reqs); this.loading.set(false); },
      error: () => { this.loading.set(false); this.errorMsg.set('loadFailed'); },
    });
  }

  filteredRequests(): LeaveRequestSummary[] {
    const email = this.filterEmail().trim().toLowerCase();
    const firstName = this.filterFirstName().trim().toLowerCase();
    const start = this.filterStartDate();
    const end = this.filterEndDate();
    return this.allRequests().filter(r => {
      if (email && !(r.employeeEmail ?? '').toLowerCase().includes(email)) return false;
      if (firstName && !(r.employeeName ?? '').toLowerCase().includes(firstName)) return false;
      if (start && r.endDate < start) return false;
      if (end && r.startDate > end) return false;
      return true;
    });
  }

  paged(): LeaveRequestSummary[] {
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

  formatDate(d: string): string {
    if (!d) return '';
    return new Date(d).toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric' });
  }
}

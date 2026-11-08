import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LeaveBalanceService, EmployeeWithBalance } from '../services/leave-balance.service';

@Component({
  selector: 'app-leave-balance-admin',
  standalone: true,
  imports: [FormsModule, DecimalPipe],
  templateUrl: './leave-balance-admin-component.html',
})
export class LeaveBalanceAdminComponent implements OnInit {
  private readonly balanceService = inject(LeaveBalanceService);

  readonly balances = signal<EmployeeWithBalance[]>([]);
  readonly loading = signal(true);
  readonly errorMsg = signal<string | null>(null);
  readonly successMsg = signal<string | null>(null);

  readonly modalUser = signal<EmployeeWithBalance | null>(null);
  readonly modalMode = signal<'add' | 'deduct'>('add');
  readonly modalDays = signal<number | null>(null);
  readonly modalSubmitting = signal(false);

  readonly searchQuery = signal('');

  readonly pageSize = 10;
  readonly currentPage = signal(1);

  readonly totalLeaveDue = computed(() =>
    this.balances().reduce((sum, b) => sum + (b.leaveDue ?? 0), 0)
  );
  readonly totalLeaveTaken = computed(() =>
    this.balances().reduce((sum, b) => sum + (b.leaveTaken ?? 0), 0)
  );
  readonly totalLeaveBalance = computed(() =>
    this.balances().reduce((sum, b) => sum + (b.leaveBalance ?? 0), 0)
  );

  readonly filteredBalances = computed(() => {
    const q = this.searchQuery().toLowerCase().trim();
    if (!q) return this.balances();
    return this.balances().filter(
      (b) =>
        b.userName.toLowerCase().includes(q) ||
        b.userEmail.toLowerCase().includes(q)
    );
  });

  readonly totalPages = computed(() =>
    Math.ceil(this.filteredBalances().length / this.pageSize)
  );

  readonly pageNumbers = computed(() =>
    Array.from({ length: this.totalPages() }, (_, i) => i + 1)
  );

  readonly pagedBalances = computed(() => {
    const start = (this.currentPage() - 1) * this.pageSize;
    return this.filteredBalances().slice(start, start + this.pageSize);
  });

  readonly modalPreview = computed(() => {
    const user = this.modalUser();
    const days = this.modalDays();
    if (!user || !days || days <= 0) return null;

    const newDue = this.modalMode() === 'add'
      ? user.leaveDue + days
      : Math.max(user.leaveDue - days, 0);
    return { oldBalance: user.leaveBalance, newBalance: Math.max(newDue - user.leaveTaken, 0) };
  });

  ngOnInit(): void {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.balanceService.listAllWithBalances().subscribe({
      next: (data) => {
        const sorted = data.sort((a, b) => {
          if (a.hasBalance !== b.hasBalance) return b.hasBalance ? 1 : -1;
          return a.userName.localeCompare(b.userName);
        });
        this.balances.set(sorted);
        this.loading.set(false);
      },
      error: () => {
        this.errorMsg.set('Impossible de charger les employés et leurs soldes.');
        this.loading.set(false);
      },
    });
  }

  onSearch(value: string): void {
    this.searchQuery.set(value);
    this.currentPage.set(1);
  }

  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages()) {
      this.currentPage.set(page);
    }
  }

  openModal(user: EmployeeWithBalance, mode: 'add' | 'deduct'): void {
    this.modalUser.set(user);
    this.modalMode.set(mode);
    this.modalDays.set(null);
  }

  closeModal(): void {
    this.modalUser.set(null);
    this.modalDays.set(null);
  }

  confirm(): void {
    const user = this.modalUser();
    const days = this.modalDays();
    if (!user || !days || days <= 0) return;

    const mode = this.modalMode();
    const action$ = mode === 'add'
      ? this.balanceService.addLeaveDue(user.userId, days)
      : this.balanceService.deductLeaveDue(user.userId, days);

    this.modalSubmitting.set(true);
    action$.subscribe({
      next: () => {
        this.successMsg.set(
          mode === 'add'
            ? `+${days} jour(s) ajouté(s) aux congés dû de ${user.userName}.`
            : `-${days} jour(s) déduit(s) des congés dû de ${user.userName}.`
        );
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

  formatDate(d: string | null): string {
    if (!d) return '—';
    return new Date(d).toLocaleDateString('fr-FR', {
      day: '2-digit', month: '2-digit', year: 'numeric',
    });
  }
}

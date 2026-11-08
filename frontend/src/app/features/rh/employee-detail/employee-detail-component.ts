import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { forkJoin } from 'rxjs';
import { InternUser } from '../../restauration/services/impl/intern.service';
import { EmployeeContextService } from '../services/employee-context.service';
import {
  EmployeeDetailsService,
  EmployeeProfileDTO,
} from '../services/employee-details.service';
import {
  LeaveService,
  LeaveBalanceInfo,
  LeaveRequestSummary,
} from '../services/leave.service';

@Component({
  selector: 'app-employee-detail',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './employee-detail-component.html',
  styleUrl: './employee-detail-component.css',
})
export class EmployeeDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly employeeContext = inject(EmployeeContextService);
  private readonly employeeDetailsService = inject(EmployeeDetailsService);
  private readonly leaveService = inject(LeaveService);

  loading = signal(true);
  error = signal<string | null>(null);
  employee = signal<EmployeeProfileDTO | null>(null);
  leaveBalances = signal<LeaveBalanceInfo[]>([]);
  leaveRequests = signal<LeaveRequestSummary[]>([]);

  /** First leave balance entry used for the main balance card */
  readonly mainBalance = computed(() => this.leaveBalances()[0] ?? null);

  /** Balance progress: available as percentage of entitled (capped 0–100) */
  readonly balancePercent = computed(() => {
    const b = this.mainBalance();
    if (!b || b.entitled <= 0) return 0;
    return Math.min(100, Math.max(0, Math.round((b.available / b.entitled) * 100)));
  });

  /** Days pending across all leave types (entitled − available) */
  readonly pendingDays = computed(() => {
    const b = this.mainBalance();
    if (!b) return 0;
    return Math.max(0, +(b.entitled - b.available).toFixed(2));
  });

  /**
   * Leave requests grouped by month (most recent first, max 5 entries).
   * Excludes cancelled and rejected requests.
   */
  /** Count of leave requests currently pending approval */
  readonly pendingRequestsCount = computed(() =>
    this.leaveRequests().filter((r) => r.overallStatus === 'En attente').length
  );

  readonly leaveByMonth = computed(() => {
    const active = this.leaveRequests().filter(
      (r) => r.overallStatus !== 'Annulé' && r.overallStatus !== 'Rejeté'
    );
    const map = new Map<string, { days: number; monthIndex: number; year: number }>();
    for (const req of active) {
      if (!req.startDate) continue;
      const d = new Date(req.startDate + 'T00:00:00');
      const label =
        d.toLocaleDateString('fr-FR', { month: 'long', year: 'numeric' });
      const key = label.charAt(0).toUpperCase() + label.slice(1);
      const existing = map.get(key);
      const days = Number(req.totalDays);
      if (existing) {
        existing.days += days;
      } else {
        map.set(key, { days, monthIndex: d.getMonth(), year: d.getFullYear() });
      }
    }
    return [...map.entries()]
      .sort((a, b) => {
        if (b[1].year !== a[1].year) return b[1].year - a[1].year;
        return b[1].monthIndex - a[1].monthIndex;
      })
      .slice(0, 5)
      .map(([label, { days }]) => ({ label, days }));
  });

  ngOnInit(): void {
    if (typeof window === 'undefined') {
      this.loading.set(false);
      this.error.set('Le profil employé est disponible côté client uniquement.');
      return;
    }

    const userIdParam = this.route.parent?.snapshot.paramMap.get('userId');
    const userId = userIdParam ? parseInt(userIdParam, 10) : null;

    if (!userId || isNaN(userId)) {
      this.loading.set(false);
      this.error.set('Aucun employé sélectionné. Retournez à la liste des employés.');
      return;
    }

    const state = (window.history.state as { user?: InternUser } | undefined)?.user;
    const contextUser = this.employeeContext.employee();
    const user = state ?? contextUser ?? { id: userId };
    this.employeeContext.setCurrentEmployee(user as InternUser);

    forkJoin({
      profile: this.employeeDetailsService.getProfileByUserId(userId),
      balances: this.leaveService.getUserLeaveBalance(userId),
      requests: this.leaveService.getUserLeaveRequests(userId),
    }).subscribe({
      next: ({ profile, balances, requests }) => {
        this.employee.set(profile);
        this.leaveBalances.set(balances);
        this.leaveRequests.set(requests);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Impossible de charger le profil employé.');
      },
    });
  }

  formatDate(value: string | undefined): string {
    if (!value) return '—';
    const d = value.split('T')[0];
    if (!d) return '—';
    const [y, m, day] = d.split('-');
    return `${day}/${m}/${y}`;
  }
}


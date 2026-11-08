import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { forkJoin } from 'rxjs';
import {
  EmployeeDetailsService,
  EmployeInfoPersonnelDTO,
} from '../services/employee-details.service';
import { UserService, User } from '../../auth/service/user.service';
import {
  LeaveService,
  LeaveBalanceInfo,
  LeaveRequestSummary,
} from '../services/leave.service';

@Component({
  selector: 'app-my-personal-info',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './my-personal-info-component.html',
  styleUrl: './my-personal-info-component.css',
})
export class MyPersonalInfoComponent implements OnInit {
  private readonly userService = inject(UserService);
  private readonly employeeDetailsService = inject(EmployeeDetailsService);
  private readonly leaveService = inject(LeaveService);

  error = signal<string | null>(null);
  loading = signal(true);
  currentUser = signal<User | null>(null);
  personalInfoData = signal<EmployeInfoPersonnelDTO | null>(null);
  leaveBalances = signal<LeaveBalanceInfo[]>([]);
  leaveRequests = signal<LeaveRequestSummary[]>([]);

  readonly fullName = computed(() => {
    const d = this.personalInfoData();
    const u = this.currentUser();
    const first = d?.firstName || u?.firstName || '';
    const last = d?.lastName || u?.lastName || '';
    return [first, last].filter(Boolean).join(' ') || '—';
  });

  readonly initials = computed(() => {
    const d = this.personalInfoData();
    const u = this.currentUser();
    const f = (d?.firstName || u?.firstName || '')[0] ?? '';
    const l = (d?.lastName || u?.lastName || '')[0] ?? '';
    return (f + l).toUpperCase() || '?';
  });

  readonly mainBalance = computed(() => this.leaveBalances()[0] ?? null);

  readonly balancePercent = computed(() => {
    const b = this.mainBalance();
    if (!b || b.entitled <= 0) return 0;
    return Math.min(100, Math.max(0, Math.round((b.available / b.entitled) * 100)));
  });

  readonly pendingDays = computed(() => {
    const b = this.mainBalance();
    if (!b) return 0;
    return Math.max(0, +(b.entitled - b.available).toFixed(2));
  });

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
      const raw = d.toLocaleDateString('fr-FR', { month: 'long', year: 'numeric' });
      const key = raw.charAt(0).toUpperCase() + raw.slice(1);
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

  private readonly maritalStatusOptions = [
    { value: 'célibataire', label: 'Célibataire' },
    { value: 'marié(e)', label: 'Marié(e)' },
  ];

  private readonly genderOptions = [
    { value: 'homme', label: 'Homme' },
    { value: 'femme', label: 'Femme' },
  ];

  ngOnInit(): void {
    this.userService.getCurrentUser().subscribe({
      next: (user) => {
        this.currentUser.set(user);
        forkJoin({
          info: this.employeeDetailsService.getByUserIdOptional(user.id),
          balances: this.leaveService.getUserLeaveBalance(user.id),
          requests: this.leaveService.getUserLeaveRequests(user.id),
        }).subscribe({
          next: ({ info, balances, requests }) => {
            this.personalInfoData.set(info);
            this.leaveBalances.set(balances);
            this.leaveRequests.set(requests);
            this.loading.set(false);
          },
          error: (err) => {
            this.error.set(err?.error?.message ?? 'Erreur lors du chargement des informations.');
            this.loading.set(false);
          },
        });
      },
      error: () => {
        this.error.set('Impossible de charger votre profil. Veuillez vous reconnecter.');
        this.loading.set(false);
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

  getMaritalStatusLabel(value: string | undefined): string {
    return this.maritalStatusOptions.find((o) => o.value === value)?.label ?? (value ?? '—');
  }

  getGenderLabel(value: string | undefined): string {
    return this.genderOptions.find((o) => o.value === value)?.label ?? (value ?? '—');
  }
}

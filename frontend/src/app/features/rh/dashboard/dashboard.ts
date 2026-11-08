import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterModule } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { DepartmentService } from '../services/department.service';
import { JobService } from '../services/job.service';
import { LeaveService } from '../services/leave.service';
import { InternService, InternUser } from '../../restauration/services/impl/intern.service';
import { SkillService } from '../services/skill.service';

interface MetricCard {
  label: string;
  value: number;
  hint: string;
  iconPath: string;
}

interface LocationStat {
  location: string;
  count: number;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class Dashboard implements OnInit {
  private readonly internService = inject(InternService);
  private readonly departmentService = inject(DepartmentService);
  private readonly leaveService = inject(LeaveService);
  private readonly jobService = inject(JobService);
  private readonly skillService = inject(SkillService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  readonly users = signal<InternUser[]>([]);
  readonly metricCards = signal<MetricCard[]>([]);
  readonly locationStats = signal<LocationStat[]>([]);

  readonly jobTitlesCount = signal(0);
  readonly jobCategoriesCount = signal(0);
  readonly leaveTypesCount = signal(0);
  readonly departmentsCount = signal(0);
  readonly qualificationsCount = signal(0);

  readonly totalStructures = computed(
    () => this.jobTitlesCount() + this.jobCategoriesCount() + this.leaveTypesCount() + this.departmentsCount()
  );

  readonly recentEmployees = computed(() =>
    [...this.users()]
      .sort((a, b) => (b.id ?? 0) - (a.id ?? 0))
      .slice(0, 6)
  );

  ngOnInit(): void {
    this.loadDashboard();
  }

  loadDashboard(): void {
    this.loading.set(true);
    this.error.set(null);

    forkJoin({
      users: this.internService.getAllUsers().pipe(catchError(() => of([]))),
      departments: this.departmentService.getAll().pipe(catchError(() => of([]))),
      leaveTypes: this.leaveService.getLeaveTypes().pipe(catchError(() => of([]))),
      jobTitles: this.jobService.getAllTitles().pipe(catchError(() => of([]))),
      jobCategories: this.jobService.getAllCategories().pipe(catchError(() => of([]))),
      qualifications: this.skillService.getAll().pipe(catchError(() => of([]))),
    }).subscribe({
      next: ({ users, departments, leaveTypes, jobTitles, jobCategories, qualifications }) => {
        this.users.set(users);
        this.departmentsCount.set(departments.length);
        this.leaveTypesCount.set(leaveTypes.length);
        this.jobTitlesCount.set(jobTitles.length);
        this.jobCategoriesCount.set(jobCategories.length);
        this.qualificationsCount.set(qualifications.length);

        this.metricCards.set([
          {
            label: 'Employes',
            value: users.length,
            hint: 'collaborateurs enregistres',
            iconPath: 'M18 20a6 6 0 0 0-12 0M12 13a5 5 0 1 0-5-5 5 5 0 0 0 5 5z',
          },
          {
            label: 'Qualifications',
            value: qualifications.length,
            hint: 'competences parametrees',
            iconPath: 'M9 12l2 2 4-4M7 4h10l3 3v13H4V4h3z',
          },
          {
            label: 'Departements',
            value: departments.length,
            hint: 'unites organisationnelles',
            iconPath: 'M4 20h16M6 16h12M8 12h8M10 8h4M5 20V6l7-3 7 3v14',
          },
          {
            label: 'Types de conges',
            value: leaveTypes.length,
            hint: 'politiques RH disponibles',
            iconPath: 'M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2z',
          },
        ]);

        this.locationStats.set(this.buildLocationStats(users));
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set('Impossible de charger les indicateurs du dashboard.');
      },
    });
  }

  getInitials(user: InternUser): string {
    const first = (user.firstName ?? '').trim().charAt(0);
    const last = (user.lastName ?? '').trim().charAt(0);
    return `${first}${last}`.toUpperCase() || user.username.slice(0, 2).toUpperCase();
  }

  private buildLocationStats(users: InternUser[]): LocationStat[] {
    const map = new Map<string, number>();

    users.forEach((user) => {
      const location = (user.location ?? '').trim() || 'Non renseigne';
      map.set(location, (map.get(location) ?? 0) + 1);
    });

    return Array.from(map.entries())
      .map(([location, count]) => ({ location, count }))
      .sort((a, b) => b.count - a.count)
      .slice(0, 6);
  }

}

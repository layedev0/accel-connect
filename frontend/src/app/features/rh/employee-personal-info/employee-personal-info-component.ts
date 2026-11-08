import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { EmployeeContextService } from '../services/employee-context.service';
import {
  EmployeeDetailsService,
  EmployeInfoPersonnelDTO,
  EmployeeDetailsRequest,
} from '../services/employee-details.service';
import { DepartmentService, Department } from '../services/department.service';
import { JobService, JobTitle, JobCategory } from '../services/job.service';
import { NotificationService } from '../../../core/services/impl/notification.service';
import { InternUser } from '../../restauration/services/impl/intern.service';

@Component({
  selector: 'app-employee-personal-info',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule],
  templateUrl: './employee-personal-info-component.html',
  styleUrls: ['./employee-personal-info-component.css'],
})
export class EmployeePersonalInfoComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly employeeContext = inject(EmployeeContextService);
  private readonly employeeDetailsService = inject(EmployeeDetailsService);
  private readonly departmentService = inject(DepartmentService);
  private readonly jobService = inject(JobService);
  private readonly notificationService = inject(NotificationService);

  form!: FormGroup;
  error = signal<string | null>(null);
  loading = signal(true);
  saving = signal(false);
  editing = signal(false);
  employeeId = signal<string>('');
  /** When set, we are in update mode; when null, we are creating. */
  employeeDetailsId = signal<number | null>(null);
  /** Cached data for read-only display when personal info exists */
  personalInfoData = signal<EmployeInfoPersonnelDTO | null>(null);
  departments = signal<Department[]>([]);
  jobTitles = signal<JobTitle[]>([]);
  jobCategories = signal<JobCategory[]>([]);
  selectedCategoryId = signal<number | null>(null);
  selectedCategory = computed(() =>
    this.jobCategories().find((c) => c.id === this.selectedCategoryId()) ?? null
  );

  contractTypeOptions = [
    { value: 'cdi', label: 'CDI' },
    { value: 'cdd', label: 'CDD' },
    { value: 'stage', label: 'STAGE' },
    { value: 'prestataire', label: 'PRESTATAIRE' },
  ];

  maritalStatusOptions = [
    { value: 'célibataire', label: 'Célibataire' },
    { value: 'marié(e)', label: 'Marié(e)' },
  ];

  genderOptions = [
    { value: 'femme', label: 'Femme' },
    { value: 'homme', label: 'Homme' },
  ];

  ngOnInit(): void {
    this.departmentService.getAll().subscribe({
      next: (list) => this.departments.set(list),
    });
    this.jobService.getAllTitles().subscribe({
      next: (list) => this.jobTitles.set(list),
    });
    this.jobService.getAllCategories().subscribe({
      next: (list) => this.jobCategories.set(list),
    });

    const userIdParam = this.route.parent?.snapshot.paramMap.get('userId');
    const userId = userIdParam ? parseInt(userIdParam, 10) : null;

    if (!userId || isNaN(userId)) {
      this.error.set(
        'Identifiant employé manquant. Retournez à la liste des employés.'
      );
      this.buildForm(null, null);
      this.loading.set(false);
      return;
    }

    const userFromContext = this.employeeContext.employee();
    const user: { id: number } = userFromContext?.id === userId ? userFromContext : { id: userId };
    this.employeeContext.setCurrentEmployee(user as InternUser);

    this.employeeId.set(userId.toString().padStart(4, '0'));
    this.employeeDetailsService.getByUserIdOptional(userId).subscribe({
      next: (data) => {
        this.buildForm(user, data);
        this.personalInfoData.set(data);
        if (data?.id != null) {
          this.employeeDetailsId.set(data.id);
          this.editing.set(false);
        } else {
          this.employeeDetailsId.set(null);
          this.editing.set(true);
        }
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(
          err?.error?.message ?? 'Erreur lors du chargement des informations.'
        );
        this.buildForm(user, null);
        this.loading.set(false);
      },
    });
  }

  private buildForm(
    user: { id: number } | null,
    data: EmployeInfoPersonnelDTO | null
  ): void {
    const empId = user ? user.id.toString().padStart(4, '0') : '';
    this.form = this.fb.group({
      employeeId: [{ value: empId, disabled: true }],
      statut: [data?.statut ?? ''],
      ssnOrId: [data?.cni ?? ''],
      telephone: [data?.telephone ?? ''],
      dateOfBirth: [this.toInputDate(data?.dateNaissance) ?? '', Validators.required],
      maritalStatus: [data?.maritalStatus ?? 'célibataire'],
      dateAdhesion: [this.toInputDate(data?.dateAdhesion) ?? '', Validators.required],
      sexe: [data?.sexe ?? 'femme', Validators.required],
      departmentId: [data?.departmentId ?? null],
      jobTitleId: [data?.jobTitleId ?? null],
      jobCategoryId: [data?.jobCategoryId ?? null],
    });
    this.selectedCategoryId.set(data?.jobCategoryId ?? null);
    this.form.get('jobCategoryId')!.valueChanges.subscribe((id) =>
      this.selectedCategoryId.set(id)
    );
  }

  private toInputDate(value: string | undefined): string {
    if (!value) return '';
    return value.split('T')[0];
  }

  private extractErrorMessage(err: { error?: { message?: string }; status?: number }): string {
    if (err?.error?.message) {
      return err.error.message;
    }
    if (err?.error && typeof err.error === 'string') {
      return err.error;
    }
    return 'Erreur lors de la sauvegarde. Veuillez vérifier les champs (ex: format du téléphone).';
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const userIdParam = this.route.parent?.snapshot.paramMap.get('userId');
    const userId = userIdParam ? parseInt(userIdParam, 10) : null;
    if (!userId || isNaN(userId)) {
      this.error.set('Identifiant employé manquant.');
      return;
    }

    const raw = this.form.getRawValue();
    const request: EmployeeDetailsRequest = {
      id: this.employeeDetailsId() ?? undefined,
      userId,
      birthday: raw.dateOfBirth || undefined,
      sexe: raw.sexe || undefined,
      maritalStatus: raw.maritalStatus || undefined,
      cni: raw.ssnOrId?.trim() || undefined,
      workTelephone: raw.telephone?.trim() || undefined,
      joinedDate: raw.dateAdhesion || undefined,
      typeContrat: raw.statut?.trim() || undefined,
      departmentId: raw.departmentId ?? undefined,
      jobTitleId: raw.jobTitleId ?? undefined,
      jobCategoryId: raw.jobCategoryId ?? undefined,
    };

    this.saving.set(true);
    this.error.set(null);
    this.employeeDetailsService.save(request).subscribe({
      next: () => {
        this.saving.set(false);
        this.notificationService.success(
          'Enregistrement réussi',
          'Les informations personnelles ont été sauvegardées.'
        );
        this.employeeDetailsService.getByUserIdOptional(userId).subscribe({
          next: (data) => {
            this.personalInfoData.set(data);
            if (data?.id != null) {
              this.employeeDetailsId.set(data.id);
              this.editing.set(false);
            }
          },
        });
      },
      error: (err) => {
        this.saving.set(false);
        const message = this.extractErrorMessage(err);
        this.notificationService.error('Erreur de validation', message);
      },
    });
  }

  startEditing(): void {
    this.editing.set(true);
  }

  cancelEditing(): void {
    this.editing.set(false);
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

  getDepartmentName(id: number | undefined): string {
    if (!id) return '—';
    return this.departments().find((d) => d.id === id)?.name ?? '—';
  }

  getJobTitleName(id: number | undefined): string {
    if (!id) return '—';
    return this.jobTitles().find((t) => t.id === id)?.jobTitle ?? '—';
  }

  getJobCategoryName(id: number | undefined): string {
    if (!id) return '—';
    return this.jobCategories().find((c) => c.id === id)?.name ?? '—';
  }
}

import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { EmployeeContextService } from '../services/employee-context.service';
import { EmployeeDetailsService } from '../services/employee-details.service';
import {
  EmployeeDependentService,
  EmployeeDependentSummary,
  EmployeeDependentRequest,
} from '../services/employee-dependent.service';
import { NotificationService } from '../../../core/services/impl/notification.service';
import { InternUser } from '../../restauration/services/impl/intern.service';

const RELATIONSHIP_OPTIONS = [
  { value: 'enfant', label: 'Enfant' },
  { value: 'conjoit(e)', label: 'Conjoint(e)' },
  { value: 'parent', label: 'Parent' },
  { value: 'Frère / Sœur', label: 'Frère / Sœur' },
  { value: 'other', label: 'Autre' },
];

@Component({
  selector: 'app-employee-dependents',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule],
  templateUrl: './employee-dependents-component.html',
  styleUrl: './employee-dependents-component.css',
})
export class EmployeeDependentsComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly employeeContext = inject(EmployeeContextService);
  private readonly employeeDetailsService = inject(EmployeeDetailsService);
  private readonly dependentService = inject(EmployeeDependentService);
  private readonly notificationService = inject(NotificationService);

  loading = signal(true);
  error = signal<string | null>(null);
  dependents = signal<EmployeeDependentSummary[]>([]);
  employeeDetailsId = signal<number | null>(null);
  showAddModal = signal(false);
  saving = signal(false);
  editingSeqNo = signal<number | null>(null);
  confirmDeleteSeqNo = signal<number | null>(null);
  deleting = signal(false);

  form!: FormGroup;
  relationshipOptions = RELATIONSHIP_OPTIONS;

  recordCount = computed(() => this.dependents().length);

  /** Link to personal-info when EmployeeDetails is missing */
  personalInfoLink = computed(() => {
    const userId = this.route.parent?.snapshot.paramMap.get('userId');
    return userId ? ['/rh-admin/employee-profile', userId, 'personal-info'] : [];
  });

  isMissingEmployeeDetails = computed(() =>
    this.error()?.includes('informations personnelles') ?? false
  );

  ngOnInit(): void {
    const userIdParam = this.route.parent?.snapshot.paramMap.get('userId');
    const userId = userIdParam ? parseInt(userIdParam, 10) : null;

    if (!userId || isNaN(userId)) {
      this.error.set("Identifiant employé manquant. Retournez à la liste des employés.");
      this.loading.set(false);
      return;
    }

    const userFromContext = this.employeeContext.employee();
    const user: { id: number } = userFromContext?.id === userId ? userFromContext : { id: userId };
    this.employeeContext.setCurrentEmployee(user as InternUser);

    this.employeeDetailsService.getByUserIdOptional(userId).subscribe({
      next: (data) => {
        if (data?.id == null) {
          this.error.set(
            "Veuillez d'abord enregistrer les informations personnelles de l'employé pour gérer les personnes à charge."
          );
          this.employeeDetailsId.set(null);
        } else {
          this.employeeDetailsId.set(data.id);
          this.loadDependents();
        }
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(
          err?.error?.message ?? "Erreur lors du chargement des informations."
        );
        this.loading.set(false);
      },
    });
  }

  private loadDependents(): void {
    const empId = this.employeeDetailsId();
    if (empId == null) return;

    this.dependentService.getAll(empId).subscribe({
      next: (list) => this.dependents.set(list),
      error: (err) => {
        this.notificationService.error(
          'Erreur',
          err?.error?.message ?? 'Impossible de charger les personnes à charge.'
        );
      },
    });
  }

  openAddModal(): void {
    this.editingSeqNo.set(null);
    this.buildForm(null);
    this.showAddModal.set(true);
  }

  openEditModal(d: EmployeeDependentSummary): void {
    this.editingSeqNo.set(d.seqNo);
    this.buildForm(d);
    this.showAddModal.set(true);
  }

  closeModal(): void {
    this.showAddModal.set(false);
    this.form?.reset();
  }

  private readonly contactNumberPattern = /^7[06785]\d{7}$/;

  private pastDateValidator(control: { value: string }) {
    if (!control.value) return null;
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return new Date(control.value) < today ? null : { pastDate: true };
  }

  private buildForm(d: EmployeeDependentSummary | null): void {
    const dateValue = d?.dateOfBirth ? this.toInputDate(d.dateOfBirth) : '';
    this.form = this.fb.group({
      name: [d?.name ?? '', [Validators.required, Validators.maxLength(100)]],
      relationship: [d?.relationship ?? '', [Validators.required, Validators.maxLength(100)]],
      dateOfBirth: [dateValue, this.pastDateValidator],
      contactNumber: [
        d?.contactNumber ?? '',
        [Validators.pattern(this.contactNumberPattern), Validators.maxLength(20)],
      ],
    });
  }

  private toInputDate(value: string): string {
    if (!value) return '';
    return value.split('T')[0];
  }

  save(): void {
    if (this.form?.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const empId = this.employeeDetailsId();
    if (empId == null) return;

    const raw = this.form.getRawValue();
    const request: EmployeeDependentRequest = {
      name: raw.name?.trim() || '',
      relationship: raw.relationship || '',
      dateOfBirth: raw.dateOfBirth || undefined,
      contactNumber: raw.contactNumber?.trim() || undefined,
    };

    this.saving.set(true);
    const seqNo = this.editingSeqNo();

    const operation = seqNo != null
      ? this.dependentService.update(empId, seqNo, request)
      : this.dependentService.create(empId, request);

    operation.subscribe({
      next: () => {
        this.saving.set(false);
        this.closeModal();
        this.loadDependents();
        this.notificationService.success(
          'Enregistrement réussi',
          seqNo != null
            ? 'La personne à charge a été mise à jour.'
            : 'La personne à charge a été ajoutée.'
        );
      },
      error: (err) => {
        this.saving.set(false);
        this.notificationService.error(
          'Erreur',
          err?.error?.message ?? 'Impossible d\'enregistrer.'
        );
      },
    });
  }

  askDelete(seqNo: number): void {
    this.confirmDeleteSeqNo.set(seqNo);
  }

  cancelDelete(): void {
    this.confirmDeleteSeqNo.set(null);
  }

  confirmDelete(): void {
    const seqNo = this.confirmDeleteSeqNo();
    const empId = this.employeeDetailsId();
    if (seqNo == null || empId == null) return;

    this.deleting.set(true);
    this.dependentService.delete(empId, seqNo).subscribe({
      next: () => {
        this.deleting.set(false);
        this.confirmDeleteSeqNo.set(null);
        this.loadDependents();
        this.notificationService.success('Supprimée', 'La personne à charge a été supprimée.');
      },
      error: (err) => {
        this.deleting.set(false);
        this.confirmDeleteSeqNo.set(null);
        this.notificationService.error('Erreur', err?.error?.message ?? 'Impossible de supprimer.');
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

  getRelationshipLabel(value: string): string {
    return this.relationshipOptions.find((o) => o.value === value)?.label ?? value;
  }
}

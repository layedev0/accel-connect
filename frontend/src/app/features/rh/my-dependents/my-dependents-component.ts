import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  EmployeeDependentService,
  EmployeeDependentSummary,
  EmployeeDependentRequest,
} from '../services/employee-dependent.service';
import { NotificationService } from '../../../core/services/impl/notification.service';

const RELATIONSHIP_OPTIONS = [
  { value: 'enfant', label: 'Enfant' },
  { value: 'conjoit(e)', label: 'Conjoint(e)' },
  { value: 'parent', label: 'Parent' },
  { value: 'Frère / Sœur', label: 'Frère / Sœur' },
  { value: 'other', label: 'Autre' },
];

@Component({
  selector: 'app-my-dependents',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './my-dependents-component.html',
})
export class MyDependentsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly dependentService = inject(EmployeeDependentService);
  private readonly notificationService = inject(NotificationService);

  loading = signal(true);
  error = signal<string | null>(null);
  dependents = signal<EmployeeDependentSummary[]>([]);
  showAddModal = signal(false);
  saving = signal(false);
  editingSeqNo = signal<number | null>(null);
  confirmDeleteSeqNo = signal<number | null>(null);
  deleting = signal(false);

  form!: FormGroup;
  relationshipOptions = RELATIONSHIP_OPTIONS;

  recordCount = computed(() => this.dependents().length);

  private readonly contactNumberPattern = /^7[0678]\d{7}$/;

  private pastDateValidator(control: { value: string }) {
    if (!control.value) return null;
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return new Date(control.value) < today ? null : { pastDate: true };
  }

  ngOnInit(): void {
    this.loadDependents();
  }

  private loadDependents(): void {
    this.loading.set(true);
    this.dependentService.getOwn().subscribe({
      next: (list) => {
        this.dependents.set(list);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'Impossible de charger les contacts.');
        this.loading.set(false);
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

  private buildForm(d: EmployeeDependentSummary | null): void {
    this.form = this.fb.group({
      name: [d?.name ?? '', [Validators.required, Validators.maxLength(100)]],
      relationship: [d?.relationship ?? '', [Validators.required, Validators.maxLength(100)]],
      dateOfBirth: [d?.dateOfBirth ? d.dateOfBirth.split('T')[0] : '', this.pastDateValidator],
      contactNumber: [
        d?.contactNumber ?? '',
        [Validators.pattern(this.contactNumberPattern), Validators.maxLength(20)],
      ],
    });
  }

  save(): void {
    if (this.form?.invalid) {
      this.form.markAllAsTouched();
      return;
    }

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
      ? this.dependentService.updateOwn(seqNo, request)
      : this.dependentService.createOwn(request);

    operation.subscribe({
      next: () => {
        this.saving.set(false);
        this.closeModal();
        this.loadDependents();
        this.notificationService.success(
          'Enregistrement réussi',
          seqNo != null ? 'Contact mis à jour.' : 'Contact ajouté.'
        );
      },
      error: (err) => {
        this.saving.set(false);
        this.notificationService.error('Erreur', err?.error?.message ?? "Impossible d'enregistrer.");
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
    if (seqNo == null) return;

    this.deleting.set(true);
    this.dependentService.deleteOwn(seqNo).subscribe({
      next: () => {
        this.deleting.set(false);
        this.confirmDeleteSeqNo.set(null);
        this.loadDependents();
        this.notificationService.success('Supprimé', 'Le contact a été supprimé.');
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

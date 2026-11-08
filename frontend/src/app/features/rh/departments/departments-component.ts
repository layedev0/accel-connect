import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { DepartmentService, Department, DepartmentRequest } from '../services/department.service';
import { NotificationService } from '../../../core/services/impl/notification.service';

@Component({
  selector: 'app-departments',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './departments-component.html',
  styleUrl: './departments-component.css',
})
export class DepartmentsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly departmentService = inject(DepartmentService);
  private readonly notificationService = inject(NotificationService);

  loading = signal(true);
  departments = signal<Department[]>([]);
  error = signal<string | null>(null);
  showModal = signal(false);
  saving = signal(false);
  deleting = signal(false);
  editingId = signal<number | null>(null);

  form!: FormGroup;

  ngOnInit(): void {
    this.loadDepartments();
  }

  loadDepartments(): void {
    this.loading.set(true);
    this.departmentService.getAll().subscribe({
      next: (list) => {
        this.departments.set(list);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.notificationService.error(
          'Erreur',
          err?.error?.message ?? 'Impossible de charger les départements.'
        );
      },
    });
  }

  openAddModal(): void {
    this.editingId.set(null);
    this.buildForm(null);
    this.showModal.set(true);
  }

  openEditModal(department: Department): void {
    this.editingId.set(department.id);
    this.buildForm(department);
    this.showModal.set(true);
  }

  closeModal(): void {
    this.showModal.set(false);
    this.form?.reset();
  }

  private buildForm(department: Department | null): void {
    this.form = this.fb.group({
      name: [department?.name ?? '', [Validators.required, Validators.maxLength(60)]],
      code: [department?.code ?? '', [Validators.required, Validators.maxLength(3), Validators.pattern('^[0-9]{1,3}$')]],
      description: [department?.description ?? '', Validators.maxLength(255)],
    });
  }

  save(): void {
    if (this.form?.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    const request: DepartmentRequest = {
      name: raw.name?.trim() || '',
      code: raw.code?.trim() || '',
      description: raw.description?.trim() || undefined,
    };

    this.saving.set(true);
    const id = this.editingId();
    const operation =
      id != null
        ? this.departmentService.update(id, request)
        : this.departmentService.create(request);

    operation.subscribe({
      next: () => {
        this.saving.set(false);
        this.closeModal();
        this.loadDepartments();
        this.notificationService.success(
          'Enregistrement réussi',
          id != null
            ? 'Le département a été mis à jour.'
            : 'Le département a été créé.'
        );
      },
      error: (err) => {
        this.saving.set(false);
        this.notificationService.error(
          'Erreur',
          err?.error?.message ?? "Impossible d'enregistrer."
        );
      },
    });
  }

  confirmDelete(department: Department): void {
    if (!confirm(`Supprimer le département « ${department.name} » ?`)) {
      return;
    }
    this.deleting.set(true);
    this.departmentService.delete(department.id).subscribe({
      next: () => {
        this.deleting.set(false);
        this.loadDepartments();
        this.notificationService.success(
          'Suppression réussie',
          'Le département a été supprimé.'
        );
      },
      error: (err) => {
        this.deleting.set(false);
        this.notificationService.error(
          'Erreur',
          err?.error?.message ??
            'Impossible de supprimer (des employés sont peut-être rattachés à ce département).'
        );
      },
    });
  }

  get modalTitle(): string {
    return this.editingId() != null ? 'Modifier le département' : 'Ajouter un département';
  }
}

import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { JobService, JobCategory, JobCategoryRequest } from '../services/job.service';
import { NotificationService } from '../../../core/services/impl/notification.service';

@Component({
  selector: 'app-job-categories',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './job-categories-component.html',
  styleUrl: './job-categories-component.css',
})
export class JobCategoriesComponent implements OnInit {
  private readonly jobService = inject(JobService);
  private readonly fb = inject(FormBuilder);
  private readonly notificationService = inject(NotificationService);

  loading = signal(true);
  saving = signal(false);
  deleting = signal<number | null>(null);
  categories = signal<JobCategory[]>([]);
  error = signal<string | null>(null);
  showModal = signal(false);
  editingId = signal<number | null>(null);
  confirmDeleteId = signal<number | null>(null);

  form!: FormGroup;

  readonly conventionScale = [
    { code: '1A',  label: '1ère A',  salaire: 70706  },
    { code: '1B',  label: '1ère B',  salaire: 74870  },
    { code: '2',   label: '2ème',    salaire: 75361  },
    { code: '3',   label: '3ème',    salaire: 77840  },
    { code: '4',   label: '4ème',    salaire: 82272  },
    { code: '5',   label: '5ème',    salaire: 89244  },
    { code: '6',   label: '6ème',    salaire: 93790  },
    { code: '7A',  label: '7ème A',  salaire: 105342 },
    { code: '7B',  label: '7ème B',  salaire: 113984 },
    { code: '8A',  label: '8ème A',  salaire: 115718 },
    { code: '8B',  label: '8ème B',  salaire: 123484 },
    { code: '8C',  label: '8ème C',  salaire: 124255 },
    { code: '9A',  label: '9ème A',  salaire: 125543 },
    { code: '9B',  label: '9ème B',  salaire: 132498 },
    { code: '10A', label: '10ème A', salaire: 141024 },
    { code: '10B', label: '10ème B', salaire: 157097 },
    { code: '10C', label: '10ème C', salaire: 174049 },
    { code: '11',  label: '11ème',   salaire: 195119 },
  ];

  onConventionChange(code: string): void {
    const entry = this.conventionScale.find((e) => e.code === code);
    if (entry) {
      this.form.patchValue({ salaireBase: entry.salaire });
    }
  }

  ngOnInit(): void {
    this.loadAll();
  }

  private loadAll(): void {
    this.loading.set(true);
    this.jobService.getAllCategories().subscribe({
      next: (list) => {
        this.categories.set(list);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement des catégories.');
        this.loading.set(false);
      },
    });
  }

  openCreate(): void {
    this.editingId.set(null);
    this.form = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(60)]],
      code: ['', [Validators.required, Validators.pattern(/^[0-9]{1,3}$/), Validators.maxLength(3)]],
      categoryCode: ['', [Validators.required, Validators.maxLength(10)]],
      salaireBase: [null],
      description: ['', Validators.maxLength(255)],
    });
    this.showModal.set(true);
  }

  openEdit(item: JobCategory): void {
    this.editingId.set(item.id);
    this.form = this.fb.group({
      name: [item.name, [Validators.required, Validators.maxLength(60)]],
      code: [item.code, [Validators.required, Validators.pattern(/^[0-9]{1,3}$/), Validators.maxLength(3)]],
      categoryCode: [item.categoryCode, [Validators.required, Validators.maxLength(10)]],
      salaireBase: [item.salaireBase ?? null],
      description: [item.description ?? '', Validators.maxLength(255)],
    });
    this.showModal.set(true);
  }

  closeModal(): void {
    this.showModal.set(false);
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    const req: JobCategoryRequest = {
      name: raw.name.trim(),
      code: raw.code.trim(),
      categoryCode: raw.categoryCode.trim(),
      salaireBase: raw.salaireBase ?? undefined,
      description: raw.description?.trim() || undefined,
    };

    this.saving.set(true);
    const id = this.editingId();
    const op$ = id ? this.jobService.updateCategory(id, req) : this.jobService.createCategory(req);

    op$.subscribe({
      next: () => {
        this.saving.set(false);
        this.showModal.set(false);
        this.notificationService.success(
          id ? 'Catégorie mise à jour' : 'Catégorie créée',
          id ? 'La catégorie a été modifiée avec succès.' : 'La catégorie a été créée avec succès.'
        );
        this.loadAll();
      },
      error: (err) => {
        this.saving.set(false);
        const msg = err?.error?.message ?? 'Erreur lors de la sauvegarde.';
        this.notificationService.error('Erreur', msg);
      },
    });
  }

  askDelete(id: number): void {
    this.confirmDeleteId.set(id);
  }

  cancelDelete(): void {
    this.confirmDeleteId.set(null);
  }

  confirmDelete(): void {
    const id = this.confirmDeleteId();
    if (!id) return;
    this.deleting.set(id);
    this.jobService.deleteCategory(id).subscribe({
      next: () => {
        this.deleting.set(null);
        this.confirmDeleteId.set(null);
        this.notificationService.success('Supprimée', 'La catégorie a été supprimée.');
        this.loadAll();
      },
      error: (err) => {
        this.deleting.set(null);
        this.confirmDeleteId.set(null);
        const msg = err?.error?.message ?? 'Impossible de supprimer cette catégorie.';
        this.notificationService.error('Erreur', msg);
      },
    });
  }
}

import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { JobService, JobTitle, JobTitleRequest } from '../services/job.service';
import { NotificationService } from '../../../core/services/impl/notification.service';

@Component({
  selector: 'app-job-titles',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './job-titles-component.html',
  styleUrl: './job-titles-component.css',
})
export class JobTitlesComponent implements OnInit {
  private readonly jobService = inject(JobService);
  private readonly fb = inject(FormBuilder);
  private readonly notificationService = inject(NotificationService);

  loading = signal(true);
  saving = signal(false);
  deleting = signal<number | null>(null);
  jobTitles = signal<JobTitle[]>([]);
  error = signal<string | null>(null);
  showModal = signal(false);
  editingId = signal<number | null>(null);
  confirmDeleteId = signal<number | null>(null);

  form!: FormGroup;

  ngOnInit(): void {
    this.loadAll();
  }

  private loadAll(): void {
    this.loading.set(true);
    this.jobService.getAllTitles().subscribe({
      next: (list) => {
        this.jobTitles.set(list);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Erreur lors du chargement des intitulés de poste.');
        this.loading.set(false);
      },
    });
  }

  openCreate(): void {
    this.editingId.set(null);
    this.form = this.fb.group({
      jobTitle: ['', [Validators.required, Validators.maxLength(60)]],
      jobTitleCode: ['', [Validators.required, Validators.pattern(/^[0-9]{1,3}$/), Validators.maxLength(3)]],
      jobDescription: ['', Validators.maxLength(255)],
    });
    this.showModal.set(true);
  }

  openEdit(item: JobTitle): void {
    this.editingId.set(item.id);
    this.form = this.fb.group({
      jobTitle: [item.jobTitle, [Validators.required, Validators.maxLength(60)]],
      jobTitleCode: [item.jobTitleCode, [Validators.required, Validators.pattern(/^[0-9]{1,3}$/), Validators.maxLength(3)]],
      jobDescription: [item.jobDescription ?? '', Validators.maxLength(255)],
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
    const req: JobTitleRequest = {
      jobTitle: raw.jobTitle.trim(),
      jobTitleCode: raw.jobTitleCode.trim(),
      jobDescription: raw.jobDescription?.trim() || undefined,
    };

    this.saving.set(true);
    const id = this.editingId();
    const op$ = id ? this.jobService.updateTitle(id, req) : this.jobService.createTitle(req);

    op$.subscribe({
      next: () => {
        this.saving.set(false);
        this.showModal.set(false);
        this.notificationService.success(
          id ? 'Intitulé mis à jour' : 'Intitulé créé',
          id ? "L'intitulé a été modifié avec succès." : "L'intitulé a été créé avec succès."
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
    this.jobService.deleteTitle(id).subscribe({
      next: () => {
        this.deleting.set(null);
        this.confirmDeleteId.set(null);
        this.notificationService.success('Supprimé', "L'intitulé a été supprimé.");
        this.loadAll();
      },
      error: (err) => {
        this.deleting.set(null);
        this.confirmDeleteId.set(null);
        const msg = err?.error?.message ?? 'Impossible de supprimer cet intitulé.';
        this.notificationService.error('Erreur', msg);
      },
    });
  }
}

import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { SkillService, Skill, SkillRequest } from '../services/skill.service';
import {
  EmployeeSkillService,
  EmployeeSkillDto,
  EmployeeSkillRequest,
} from '../services/employee-skill.service';
import { EmployeeDetailsService } from '../services/employee-details.service';
import { NotificationService } from '../../../core/services/impl/notification.service';
import { UserService } from '../../auth/service/user.service';

@Component({
  selector: 'app-skills',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './skills-component.html',
  styleUrl: './skills-component.css',
})
export class SkillsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly skillService = inject(SkillService);
  private readonly employeeSkillService = inject(EmployeeSkillService);
  private readonly employeeDetailsService = inject(EmployeeDetailsService);
  private readonly route = inject(ActivatedRoute);
  private readonly notificationService = inject(NotificationService);
  private readonly userService = inject(UserService);

  loading = signal(true);
  skills = signal<EmployeeSkillDto[]>([]);
  allSkills = signal<Skill[]>([]);
  adminSkills = signal<Skill[]>([]);
  error = signal<string | null>(null);
  employeeDetailsId = signal<number | null>(null);
  currentUserId = signal<number | null>(null);
  isAdminView = signal(false);
  isQualificationAdmin = signal(false);
  showModal = signal(false);
  saving = signal(false);
  deleting = signal(false);
  editingId = signal<number | null>(null);

  form!: FormGroup;

  ngOnInit(): void {
    const userIdParam = this.route.parent?.snapshot.paramMap.get('userId');
    const userId = userIdParam ? parseInt(userIdParam, 10) : null;

    const routePath = this.route.snapshot.routeConfig?.path;
    const isQualifications = routePath === 'qualifications';
    this.isQualificationAdmin.set(isQualifications);
    this.isAdminView.set(!!userIdParam || isQualifications);

    if (isQualifications) {
      this.loadQualifications();
      return;
    }

    this.loadSkillOptions();

    if (!userId || isNaN(userId)) {
      this.userService.getCurrentUser().subscribe({
        next: (user) => {
          this.currentUserId.set(user.id);
          this.loadEmployeeDetails(user.id);
        },
        error: (err) => {
          this.error.set(
            err?.error?.message ?? 'Impossible de charger vos informations.'
          );
          this.loading.set(false);
        },
      });
      return;
    }

    this.currentUserId.set(userId);
    this.loadEmployeeDetails(userId);
  }

  loadQualifications(): void {
    this.loading.set(true);
    this.skillService.getAll().subscribe({
      next: (list) => {
        this.adminSkills.set(list);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.notificationService.error(
          'Erreur',
          err?.error?.message ?? 'Impossible de charger les qualifications.'
        );
      },
    });
  }

  private loadEmployeeDetails(userId: number): void {
    this.employeeDetailsService.getByUserIdOptional(userId).subscribe({
      next: (data) => {
        if (data?.id == null) {
          this.error.set(
            'Aucune fiche employé disponible. Veuillez compléter les informations personnelles.'
          );
          this.loading.set(false);
          return;
        }
        this.employeeDetailsId.set(data.id);
        this.loadEmployeeSkills(data.id);
      },
      error: (err) => {
        this.error.set(
          err?.error?.message ?? 'Impossible de charger les informations employé.'
        );
        this.loading.set(false);
      },
    });
  }

  loadEmployeeSkills(employeeDetailsId: number): void {
    this.loading.set(true);
    this.employeeSkillService.getAll(employeeDetailsId).subscribe({
      next: (list) => {
        this.skills.set(list);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.notificationService.error(
          'Erreur',
          err?.error?.message ?? 'Impossible de charger les compétences.'
        );
      },
    });
  }

  loadSkillOptions(): void {
    this.skillService.getAll().subscribe({
      next: (list) => {
        this.allSkills.set(list);
      },
      error: (err) => {
        this.notificationService.error(
          'Erreur',
          err?.error?.message ?? 'Impossible de charger la liste des compétences.'
        );
      },
    });
  }

  openAddModal(): void {
    this.editingId.set(null);
    if (this.isQualificationAdmin()) {
      this.buildAdminForm(null);
    } else {
      this.buildForm(null);
    }
    this.showModal.set(true);
  }

  openEditModal(skill: EmployeeSkillDto | Skill): void {
    if (this.isQualificationAdmin()) {
      const s = skill as Skill;
      this.editingId.set(s.id);
      this.buildAdminForm(s);
    } else {
      const s = skill as EmployeeSkillDto;
      this.editingId.set(s.skillId);
      this.buildForm(s);
    }
    this.showModal.set(true);
  }

  closeModal(): void {
    this.showModal.set(false);
    this.form?.reset();
  }

  private buildForm(skill: EmployeeSkillDto | null): void {
    this.form = this.fb.group({
      skillId: [
        { value: skill?.skillId ?? null, disabled: skill != null },
        Validators.required,
      ],
      yearsOfExperience: [skill?.yearsOfExperience ?? null, Validators.min(0)],
      comments: [skill?.comments ?? '', Validators.maxLength(500)],
    });
  }

  private buildAdminForm(skill: Skill | null): void {
    this.form = this.fb.group({
      name: [skill?.name ?? '', [Validators.required, Validators.maxLength(120)]],
      description: [skill?.description ?? '', Validators.maxLength(1000)],
    });
  }

  save(): void {
    if (this.isQualificationAdmin()) {
      if (this.form?.invalid) {
        this.form.markAllAsTouched();
        return;
      }

      const raw = this.form.getRawValue();
      const request: SkillRequest = {
        name: raw.name?.trim() || '',
        description: raw.description?.trim() || undefined,
      };

      this.saving.set(true);
      const id = this.editingId();
      const operation =
        id != null
          ? this.skillService.update(id, request)
          : this.skillService.create(request);

      operation.subscribe({
        next: () => {
          this.saving.set(false);
          this.closeModal();
          this.loadQualifications();
          this.notificationService.success(
            'Enregistrement réussi',
            id != null
              ? 'La qualification a été mise à jour.'
              : 'La qualification a été ajoutée.'
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
      return;
    }

    if (this.form?.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const detailsId = this.employeeDetailsId();
    if (!detailsId) {
      this.notificationService.error(
        'Erreur',
        'Fiche employé manquante. Veuillez compléter les informations personnelles.'
      );
      return;
    }

    const raw = this.form.getRawValue();
    const skillId = raw.skillId as number | null;
    if (!skillId) {
      this.form.get('skillId')?.markAsTouched();
      return;
    }

    const request: EmployeeSkillRequest = {
      skillId,
      yearsOfExperience: raw.yearsOfExperience ?? undefined,
      comments: raw.comments?.trim() || undefined,
    };

    this.saving.set(true);
    const id = this.editingId();
    const operation =
      id != null
        ? this.employeeSkillService.update(detailsId, id, request)
        : this.employeeSkillService.create(detailsId, request);

    operation.subscribe({
      next: () => {
        this.saving.set(false);
        this.closeModal();
        this.loadEmployeeSkills(detailsId);
        this.notificationService.success(
          'Enregistrement réussi',
          id != null
            ? 'La compétence a été mise à jour.'
            : 'La compétence a été ajoutée.'
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

  confirmDelete(skill: EmployeeSkillDto | Skill): void {
    if (this.isQualificationAdmin()) {
      const s = skill as Skill;
      if (!confirm(`Supprimer la qualification « ${s.name} » ?`)) {
        return;
      }
      this.deleting.set(true);
      this.skillService.delete(s.id).subscribe({
        next: () => {
          this.deleting.set(false);
          this.loadQualifications();
          this.notificationService.success(
            'Suppression réussie',
            'La qualification a été supprimée.'
          );
        },
        error: (err) => {
          this.deleting.set(false);
          this.notificationService.error(
            'Erreur',
            err?.error?.message ??
              'Impossible de supprimer (qualification peut-être assignée à des employés).'
          );
        },
      });
      return;
    }

    const s = skill as EmployeeSkillDto;
    const name = this.getSkillName(s);
    if (!confirm(`Supprimer la compétence « ${name} » ?`)) {
      return;
    }
    const detailsId = this.employeeDetailsId();
    if (!detailsId) {
      this.notificationService.error(
        'Erreur',
        'Fiche employé manquante. Veuillez compléter les informations personnelles.'
      );
      return;
    }
    this.deleting.set(true);
    this.employeeSkillService.delete(detailsId, s.skillId).subscribe({
      next: () => {
        this.deleting.set(false);
        this.loadEmployeeSkills(detailsId);
        this.notificationService.success(
          'Suppression réussie',
          'La compétence a été supprimée.'
        );
      },
      error: (err) => {
        this.deleting.set(false);
        this.notificationService.error(
          'Erreur',
          err?.error?.message ?? 'Impossible de supprimer (compétence peut-être assignée à des employés).'
        );
      },
    });
  }

  get modalTitle(): string {
    if (this.isQualificationAdmin()) {
      return this.editingId() != null ? 'Modifier la qualification' : 'Ajouter une qualification';
    }
    return this.editingId() != null ? 'Modifier la compétence' : 'Ajouter une compétence';
  }

  getSkillName(skill: EmployeeSkillDto): string {
    return (
      skill.skillName ??
      this.allSkills().find((s) => s.id === skill.skillId)?.name ??
      '—'
    );
  }
}

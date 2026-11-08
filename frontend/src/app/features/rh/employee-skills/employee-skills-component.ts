import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { EmployeeContextService } from '../services/employee-context.service';
import { EmployeeDetailsService } from '../services/employee-details.service';
import { EmployeeSkillDto, EmployeeSkillRequest, EmployeeSkillService } from '../services/employee-skill.service';
import { Skill, SkillService } from '../services/skill.service';
import { NotificationService } from '../../../core/services/impl/notification.service';
import { InternUser } from '../../restauration/services/impl/intern.service';

@Component({
  selector: 'app-employee-skills',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule],
  templateUrl: './employee-skills-component.html',
  styleUrl: './employee-skills-component.css',
})
export class EmployeeSkillsComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly employeeContext = inject(EmployeeContextService);
  private readonly employeeDetailsService = inject(EmployeeDetailsService);
  private readonly employeeSkillService = inject(EmployeeSkillService);
  private readonly skillService = inject(SkillService);
  private readonly notificationService = inject(NotificationService);

  loading = signal(true);
  error = signal<string | null>(null);
  employeeDetailsId = signal<number | null>(null);

  skillsLoading = signal(false);
  skills = signal<EmployeeSkillDto[]>([]);
  availableSkills = signal<Skill[]>([]);

  form!: FormGroup;
  saving = signal(false);
  editingSkillId = signal<number | null>(null);
  showForm = signal(false);

  ngOnInit(): void {
    const userIdParam = this.route.parent?.snapshot.paramMap.get('userId');
    const userId = userIdParam ? parseInt(userIdParam, 10) : null;

    if (!userId || isNaN(userId)) {
      this.error.set('Identifiant employé manquant. Retournez à la liste des employés.');
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
            'Veuillez d’abord enregistrer les informations personnelles pour gérer les compétences.'
          );
          this.employeeDetailsId.set(null);
        } else {
          this.employeeDetailsId.set(data.id);
          this.initSkillForm(null);
          this.loadEmployeeSkills();
          this.loadAvailableSkills();
        }
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(
          err?.error?.message ?? 'Erreur lors du chargement des informations.'
        );
        this.loading.set(false);
      },
    });
  }

  private initSkillForm(skill: EmployeeSkillDto | null): void {
    this.form = this.fb.group({
      skillId: [skill?.skillId ?? null, Validators.required],
      yearsOfExperience: [
        skill?.yearsOfExperience ?? null,
        [Validators.min(0), Validators.max(50)],
      ],
      comments: [skill?.comments ?? null, Validators.maxLength(100)],
    });
  }

  private loadEmployeeSkills(): void {
    const employeeDetailsId = this.employeeDetailsId();
    if (employeeDetailsId == null) return;

    this.skillsLoading.set(true);
    this.employeeSkillService.getAll(employeeDetailsId).subscribe({
      next: (list) => {
        this.skills.set(list);
        this.skillsLoading.set(false);
      },
      error: (err) => {
        this.skillsLoading.set(false);
        this.notificationService.error(
          'Erreur',
          err?.error?.message ?? 'Impossible de charger les compétences.'
        );
      },
    });
  }

  private loadAvailableSkills(): void {
    this.skillService.getAll().subscribe({
      next: (list) => this.availableSkills.set(list),
      error: (err) => {
        this.notificationService.error(
          'Erreur',
          err?.error?.message ?? 'Impossible de charger la liste des compétences.'
        );
      },
    });
  }

  startAdd(): void {
    this.editingSkillId.set(null);
    this.initSkillForm(null);
    this.showForm.set(true);
  }

  startEdit(skill: EmployeeSkillDto): void {
    this.editingSkillId.set(skill.skillId);
    this.initSkillForm(skill);
    this.form.get('skillId')?.disable();
    this.showForm.set(true);
  }

  cancelEdit(): void {
    this.editingSkillId.set(null);
    this.initSkillForm(null);
    this.showForm.set(false);
  }

  save(): void {
    if (this.form?.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const employeeDetailsId = this.employeeDetailsId();
    if (employeeDetailsId == null) return;

    const raw = this.form.getRawValue();
    const request: EmployeeSkillRequest = {
      skillId: raw.skillId,
      yearsOfExperience: raw.yearsOfExperience ?? undefined,
      comments: raw.comments?.trim() || null,
    };

    this.saving.set(true);
    const isEdit = this.editingSkillId() != null;
    const skillId = this.editingSkillId() ?? raw.skillId;
    const operation = isEdit
      ? this.employeeSkillService.update(employeeDetailsId, skillId, request)
      : this.employeeSkillService.create(employeeDetailsId, request);

    operation.subscribe({
      next: () => {
        this.saving.set(false);
        this.cancelEdit();
        this.loadEmployeeSkills();
        this.notificationService.success(
          'Enregistrement réussi',
          isEdit ? 'La compétence a été mise à jour.' : 'La compétence a été ajoutée.'
        );
      },
      error: (err) => {
        this.saving.set(false);
        this.notificationService.error(
          'Erreur',
          err?.error?.message ?? 'Impossible d’enregistrer la compétence.'
        );
      },
    });
  }

  deleteSkill(skill: EmployeeSkillDto): void {
    const employeeDetailsId = this.employeeDetailsId();
    if (employeeDetailsId == null) return;
    if (!confirm(`Supprimer la compétence « ${this.getSkillLabel(skill)} » ?`)) {
      return;
    }

    this.employeeSkillService.delete(employeeDetailsId, skill.skillId).subscribe({
      next: () => {
        this.loadEmployeeSkills();
        this.notificationService.success(
          'Suppression réussie',
          'La compétence a été supprimée.'
        );
      },
      error: (err) => {
        this.notificationService.error(
          'Erreur',
          err?.error?.message ?? 'Impossible de supprimer la compétence.'
        );
      },
    });
  }

  getSkillLabel(skill: EmployeeSkillDto): string {
    if (skill.skillName) return skill.skillName;
    const found = this.availableSkills().find((s) => s.id === skill.skillId);
    return found?.name ?? `#${skill.skillId}`;
  }
}

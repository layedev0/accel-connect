import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { EmployeeSkillDto, EmployeeSkillRequest, EmployeeSkillService } from '../services/employee-skill.service';
import { Skill, SkillService } from '../services/skill.service';
import { NotificationService } from '../../../core/services/impl/notification.service';

@Component({
  selector: 'app-my-skills',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './my-skills-component.html',
})
export class MySkillsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly employeeSkillService = inject(EmployeeSkillService);
  private readonly skillService = inject(SkillService);
  private readonly notificationService = inject(NotificationService);

  loading = signal(true);
  error = signal<string | null>(null);

  skillsLoading = signal(false);
  skills = signal<EmployeeSkillDto[]>([]);
  availableSkills = signal<Skill[]>([]);

  form!: FormGroup;
  saving = signal(false);
  editingSkillId = signal<number | null>(null);
  showForm = signal(false);

  ngOnInit(): void {
    this.initSkillForm(null);
    this.loadEmployeeSkills();
    this.loadAvailableSkills();
    this.loading.set(false);
  }

  private initSkillForm(skill: EmployeeSkillDto | null): void {
    this.form = this.fb.group({
      skillId: [skill?.skillId ?? null, Validators.required],
      yearsOfExperience: [skill?.yearsOfExperience ?? null, [Validators.min(0), Validators.max(50)]],
      comments: [skill?.comments ?? null, Validators.maxLength(100)],
    });
  }

  private loadEmployeeSkills(): void {
    this.skillsLoading.set(true);
    this.employeeSkillService.getOwn().subscribe({
      next: (list) => {
        this.skills.set(list);
        this.skillsLoading.set(false);
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'Impossible de charger les compétences.');
        this.skillsLoading.set(false);
      },
    });
  }

  private loadAvailableSkills(): void {
    this.skillService.getAll().subscribe({
      next: (list) => this.availableSkills.set(list),
      error: (err) => {
        this.notificationService.error('Erreur', err?.error?.message ?? 'Impossible de charger la liste des compétences.');
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
      ? this.employeeSkillService.updateOwn(skillId, request)
      : this.employeeSkillService.createOwn(request);

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
        this.notificationService.error('Erreur', err?.error?.message ?? "Impossible d'enregistrer la compétence.");
      },
    });
  }

  deleteSkill(skill: EmployeeSkillDto): void {
    if (!confirm(`Supprimer la compétence « ${this.getSkillLabel(skill)} » ?`)) return;

    this.employeeSkillService.deleteOwn(skill.skillId).subscribe({
      next: () => {
        this.loadEmployeeSkills();
        this.notificationService.success('Suppression réussie', 'La compétence a été supprimée.');
      },
      error: (err) => {
        this.notificationService.error('Erreur', err?.error?.message ?? 'Impossible de supprimer la compétence.');
      },
    });
  }

  getSkillLabel(skill: EmployeeSkillDto): string {
    if (skill.skillName) return skill.skillName;
    return this.availableSkills().find((s) => s.id === skill.skillId)?.name ?? `#${skill.skillId}`;
  }
}

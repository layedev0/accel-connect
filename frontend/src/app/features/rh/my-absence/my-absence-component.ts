import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { UserService } from '../../auth/service/user.service';
import {
  AbsenceService,
  AbsenceSummary,
  AbsenceRequest,
  AbsenceType,
} from '../services/absence.service';
import { LeaveService, ApproverOption } from '../services/leave.service';
import { NgTemplateOutlet } from '@angular/common';

type AbsenceTab = 'pending' | 'approved' | 'rejected';

@Component({
  selector: 'app-my-absence',
  standalone: true,
  imports: [ReactiveFormsModule, NgTemplateOutlet],
  templateUrl: './my-absence-component.html',
})
export class MyAbsenceComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly userService = inject(UserService);
  private readonly absenceService = inject(AbsenceService);
  private readonly leaveService = inject(LeaveService);

  readonly currentUserId = signal<number | null>(null);
  readonly absences = signal<AbsenceSummary[]>([]);
  readonly absenceTypes = signal<AbsenceType[]>([]);
  readonly loading = signal(true);
  readonly showForm = signal(false);
  readonly submitting = signal(false);
  readonly editingAbsence = signal<AbsenceSummary | null>(null);
  readonly errorMsg = signal<string | null>(null);
  readonly successMsg = signal<string | null>(null);
  readonly expandedId = signal<number | null>(null);

  // Tabs
  readonly activeTab = signal<AbsenceTab>('pending');

  readonly pendingAbsences = computed(() =>
    this.absences().filter(a => a.status === 'En attente')
  );
  readonly approvedAbsences = computed(() =>
    this.absences().filter(a => a.status === 'Approuvé')
  );
  readonly rejectedAbsences = computed(() =>
    this.absences().filter(a => a.status === 'Rejeté')
  );
  readonly activeTabAbsences = computed(() => {
    switch (this.activeTab()) {
      case 'pending':  return this.pendingAbsences();
      case 'approved': return this.approvedAbsences();
      case 'rejected': return this.rejectedAbsences();
    }
  });

  setTab(tab: AbsenceTab): void {
    this.activeTab.set(tab);
    this.currentPage.set(1);
    this.expandedId.set(null);
  }

  // Pagination (per active tab)
  readonly pageSize = 10;
  readonly currentPage = signal(1);
  readonly totalPages = computed(() =>
    Math.max(1, Math.ceil(this.activeTabAbsences().length / this.pageSize))
  );
  readonly pagedAbsences = computed(() => {
    const start = (this.currentPage() - 1) * this.pageSize;
    return this.activeTabAbsences().slice(start, start + this.pageSize);
  });
  readonly pages = computed(() =>
    Array.from({ length: this.totalPages() }, (_, i) => i + 1)
  );

  // Approver single-select (reuses colleagues from leave service)
  readonly allColleagues = signal<ApproverOption[]>([]);
  readonly selectedApproverId = signal<number | null>(null);
  readonly approverSearch = signal('');
  readonly showApproverDropdown = signal(false);
  readonly approverTouched = signal(false);
  readonly approverError = computed(() => this.approverTouched() && this.selectedApproverId() === null);

  readonly filteredColleagues = computed(() => {
    const q = this.approverSearch().toLowerCase();
    const currentId = this.currentUserId();
    return this.allColleagues().filter(
      (c) =>
        c.id !== currentId &&
        (c.email.toLowerCase().includes(q) ||
          `${c.firstName} ${c.lastName}`.toLowerCase().includes(q))
    );
  });

  readonly selectedApprover = computed(() =>
    this.allColleagues().find((c) => c.id === this.selectedApproverId()) ?? null
  );

  readonly form = this.fb.group({
    absenceType: [null as string | null, Validators.required],
    date: ['', Validators.required],
    startTime: ['', Validators.required],
    endTime: ['', Validators.required],
    reason: ['', Validators.required],
  });

  private readonly formValues = toSignal(this.form.valueChanges, {
    initialValue: this.form.value,
  });

  readonly todayStr: string = (() => {
    const d = new Date();
    return [
      d.getFullYear(),
      String(d.getMonth() + 1).padStart(2, '0'),
      String(d.getDate()).padStart(2, '0'),
    ].join('-');
  })();

  readonly timeSlots: string[] = Array.from({ length: 48 }, (_, i) => {
    const h = Math.floor(i / 2);
    const m = i % 2 === 0 ? '00' : '30';
    return `${String(h).padStart(2, '0')}:${m}`;
  });

  readonly durationError = computed((): string | null => {
    const { startTime, endTime } = this.formValues();
    if (!startTime || !endTime) return null;
    if (endTime <= startTime) return "L'heure de fin doit être après l'heure de début.";
    return null;
  });

  readonly estimatedDuration = computed((): string | null => {
    const { startTime, endTime } = this.formValues();
    if (!startTime || !endTime || endTime <= startTime) return null;
    const [sh, sm] = startTime.split(':').map(Number);
    const [eh, em] = endTime.split(':').map(Number);
    const minutes = (eh * 60 + em) - (sh * 60 + sm);
    const h = Math.floor(minutes / 60);
    const m = minutes % 60;
    return m === 0 ? `${h}h` : `${h}h${String(m).padStart(2, '0')}`;
  });

  ngOnInit(): void {
    this.userService.getCurrentUser().subscribe({
      next: (user) => {
        this.currentUserId.set(user.id);
        this.loadAbsences(user.id);
        this.absenceService.getAbsenceTypes().subscribe({
          next: (types) => this.absenceTypes.set(types),
          error: () => {},
        });
        this.leaveService.getColleagues().subscribe({
          next: (c) => this.allColleagues.set(c),
          error: () => {},
        });
      },
      error: () => this.loading.set(false),
    });
  }

  private loadAbsences(userId: number): void {
    this.loading.set(true);
    this.absenceService.getUserAbsences(userId).subscribe({
      next: (list) => {
        this.absences.set(list);
        this.currentPage.set(1);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  goToPage(page: number): void {
    if (page < 1 || page > this.totalPages()) return;
    this.currentPage.set(page);
    this.expandedId.set(null);
  }

  pageEnd(): number {
    return Math.min(this.currentPage() * this.pageSize, this.activeTabAbsences().length);
  }

  toggleForm(): void {
    this.showForm.update((v) => !v);
    if (!this.showForm()) {
      this.resetForm();
    }
  }

  openEdit(abs: AbsenceSummary): void {
    this.editingAbsence.set(abs);
    this.form.setValue({
      absenceType: abs.absenceTypeName ?? null,
      date: abs.date,
      startTime: abs.startTime.substring(0, 5),
      endTime: abs.endTime.substring(0, 5),
      reason: abs.reason ?? '',
    });
    this.selectedApproverId.set(abs.approvers?.[0]?.id ?? null);
    this.errorMsg.set(null);
    this.showForm.set(true);
  }

  private resetForm(): void {
    this.form.reset();
    this.editingAbsence.set(null);
    this.errorMsg.set(null);
    this.selectedApproverId.set(null);
    this.approverSearch.set('');
    this.showApproverDropdown.set(false);
    this.approverTouched.set(false);
  }

  toggleApproverDropdown(): void {
    this.showApproverDropdown.update((v) => !v);
    if (this.showApproverDropdown()) this.approverSearch.set('');
  }

  toggleApprover(id: number): void {
    this.selectedApproverId.set(id);
    this.showApproverDropdown.set(false);
  }

  removeApprover(): void {
    this.selectedApproverId.set(null);
  }

  closeApproverDropdown(): void {
    this.showApproverDropdown.set(false);
    this.approverTouched.set(true);
  }

  toggleExpand(id: number): void {
    this.expandedId.update((cur) => (cur === id ? null : id));
  }

  onSubmit(): void {
    this.approverTouched.set(true);
    if (this.form.invalid || this.submitting() || this.durationError() || this.selectedApproverId() === null) return;
    const userId = this.currentUserId();
    if (!userId) return;

    const v = this.form.value;
    const payload: AbsenceRequest = {
      absenceType: v.absenceType!,
      date: v.date!,
      startTime: v.startTime!,
      endTime: v.endTime!,
      reason: v.reason!,
    };

    const approverId = this.selectedApproverId();
    if (approverId !== null) payload.approverUserIds = [approverId];

    this.submitting.set(true);
    this.errorMsg.set(null);

    const editing = this.editingAbsence();
    const op$ = editing
      ? this.absenceService.updateAbsence(editing.id, userId, payload)
      : this.absenceService.submitAbsence(userId, payload);

    op$.subscribe({
      next: () => {
        this.submitting.set(false);
        this.showForm.set(false);
        this.resetForm();
        this.successMsg.set(editing ? 'Demande d\'absence modifiée avec succès.' : 'Demande d\'absence soumise avec succès.');
        this.loadAbsences(userId);
        setTimeout(() => this.successMsg.set(null), 4000);
      },
      error: (err) => {
        this.errorMsg.set(err?.error?.message ?? 'Une erreur est survenue.');
        this.submitting.set(false);
      },
    });
  }

  deleteAbsence(id: number): void {
    if (!confirm('Supprimer cette demande d\'absence ? Cette action est irréversible.')) return;
    const userId = this.currentUserId();
    if (!userId) return;
    this.absenceService.deleteAbsence(id, userId).subscribe({
      next: () => this.loadAbsences(userId),
      error: (err) =>
        this.errorMsg.set(err?.error?.message ?? 'Impossible de supprimer la demande.'),
    });
  }

  statusClass(status: string): string {
    switch (status) {
      case 'En attente': return 'bg-amber-100 text-amber-800';
      case 'Approuvé':   return 'bg-green-100 text-green-800';
      case 'Rejeté':     return 'bg-red-100 text-red-800';
      default:           return 'bg-gray-100 text-gray-500';
    }
  }

  formatDate(d: string): string {
    if (!d) return '';
    return new Date(d).toLocaleDateString('fr-FR', {
      day: '2-digit', month: '2-digit', year: 'numeric',
    });
  }

  formatTime(t: string): string {
    if (!t) return '';
    return t.substring(0, 5); // HH:mm
  }
}

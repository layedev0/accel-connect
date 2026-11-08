import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, concatMap, forkJoin, from, of, toArray } from 'rxjs';
import { UserService } from '../../auth/service/user.service';
import { EmployeeDetailsService, EmployeInfoPersonnelDTO } from '../services/employee-details.service';
import {
  LeaveService,
  LeaveType,
  LeaveBalanceInfo,
  LeaveRequestSummary,
  LeaveApplicationRequest,
  LeaveApplicationResponse,
  ApproverOption,
} from '../services/leave.service';
import { LeaveBalanceService, UserLeaveBalance } from '../services/leave-balance.service';
import { AttachmentService, AttachmentDTO } from '../services/attachment.service';
import { DecimalPipe, NgTemplateOutlet } from '@angular/common';

type Tab = 'pending' | 'approved' | 'rejected';

const APPROVED_STATUSES = new Set(['Approuvé', 'Pris']);
const REJECTED_STATUSES = new Set(['Rejeté', 'Annulé']);

@Component({
  selector: 'app-my-leave',
  standalone: true,
  imports: [ReactiveFormsModule, DecimalPipe, NgTemplateOutlet],
  templateUrl: './my-leave-component.html',
})
export class MyLeaveComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly userService = inject(UserService);
  private readonly employeeService = inject(EmployeeDetailsService);
  private readonly leaveService = inject(LeaveService);
  private readonly leaveBalanceService = inject(LeaveBalanceService);
  private readonly attachmentService = inject(AttachmentService);

  readonly currentUserId = signal<number | null>(null);
  readonly employeeInfo = signal<EmployeInfoPersonnelDTO | null>(null);
  readonly leaveTypes = signal<LeaveType[]>([]);
  readonly balances = signal<LeaveBalanceInfo[]>([]);
  readonly userBalance = signal<UserLeaveBalance | null>(null);
  readonly requests = signal<LeaveRequestSummary[]>([]);
  readonly loading = signal(true);
  readonly showForm = signal(false);
  readonly submitting = signal(false);
  readonly errorMsg = signal<string | null>(null);
  readonly successMsg = signal<string | null>(null);
  readonly expandedRequestId = signal<number | null>(null);
  readonly editingRequest = signal<LeaveRequestSummary | null>(null);

  // Tabs
  readonly activeTab = signal<Tab>('pending');

  readonly pendingRequests = computed(() =>
    this.requests().filter(r => r.overallStatus === 'En attente')
  );
  readonly approvedRequests = computed(() =>
    this.requests().filter(r => APPROVED_STATUSES.has(r.overallStatus))
  );
  readonly rejectedRequests = computed(() =>
    this.requests().filter(r => REJECTED_STATUSES.has(r.overallStatus))
  );
  readonly activeTabRequests = computed(() => {
    switch (this.activeTab()) {
      case 'pending':  return this.pendingRequests();
      case 'approved': return this.approvedRequests();
      case 'rejected': return this.rejectedRequests();
    }
  });

  setTab(tab: Tab): void {
    this.activeTab.set(tab);
    this.currentPage.set(1);
    this.expandedRequestId.set(null);
  }

  // Pagination (per active tab)
  readonly pageSize = 10;
  readonly currentPage = signal(1);
  readonly totalPages = computed(() =>
    Math.ceil(this.activeTabRequests().length / this.pageSize)
  );
  readonly paginatedRequests = computed(() => {
    const start = (this.currentPage() - 1) * this.pageSize;
    return this.activeTabRequests().slice(start, start + this.pageSize);
  });
  readonly pageNumbers = computed(() =>
    Array.from({ length: this.totalPages() }, (_, i) => i + 1)
  );
  pageEnd(): number {
    return Math.min(this.currentPage() * this.pageSize, this.activeTabRequests().length);
  }

  // File attachments (newly staged, not yet uploaded)
  readonly attachedFiles = signal<File[]>([]);
  readonly attachFileError = signal<string | null>(null);

  // Existing attachments loaded when editing a pending request
  readonly existingAttachments = signal<AttachmentDTO[]>([]);
  readonly removedAttachmentIds = signal<Set<number>>(new Set());
  readonly loadingExistingAttachments = signal(false);

  readonly keptAttachments = computed(() =>
    this.existingAttachments().filter((a) => !this.removedAttachmentIds().has(a.attachId))
  );

  private static readonly MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
  private static readonly ALLOWED_TYPES = ['application/pdf', 'image/jpeg', 'image/png'];

  // Approver single-select (required)
  readonly allColleagues = signal<ApproverOption[]>([]);
  readonly selectedApproverId = signal<number | null>(null);
  readonly approverSearch = signal('');
  readonly showApproverDropdown = signal(false);

  readonly selectedNotifyIds = signal<Set<number>>(new Set());
  readonly notifySearch = signal('');
  readonly showNotifyDropdown = signal(false);

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

  readonly filteredNotifyColleagues = computed(() => {
    const q = this.notifySearch().toLowerCase();
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

  readonly selectedNotifyUsers = computed(() =>
    this.allColleagues().filter((c) => this.selectedNotifyIds().has(c.id))
  );

  readonly form = this.fb.group({
    leaveTypeCode: [null as string | null, Validators.required],
    startDate: ['', Validators.required],
    endDate: ['', Validators.required],
    startTime: [''],
    endTime: [''],
    comment: [''],
  });

  private readonly formValues = toSignal(this.form.valueChanges, {
    initialValue: this.form.value,
  });

  /** Today as YYYY-MM-DD (local date, used as min for date inputs) */
  readonly todayStr: string = (() => {
    const d = new Date();
    return [
      d.getFullYear(),
      String(d.getMonth() + 1).padStart(2, '0'),
      String(d.getDate()).padStart(2, '0'),
    ].join('-');
  })();

  /** Minimum allowed end date = start date (if selected) */
  readonly minEndDate = computed(() => this.formValues().startDate || '');

  readonly selectedBalance = computed(() => {
    const typeCode = this.formValues().leaveTypeCode;
    if (!typeCode) return null;
    return this.balances().find((b) => b.leaveTypeCode === typeCode) ?? null;
  });

  /** True when start and end date are the same working day */
  readonly isSingleDay = computed(() => {
    const { startDate, endDate } = this.formValues();
    return !!startDate && !!endDate && startDate === endDate;
  });

  /** Duration in hours for a single-day time range (null if not applicable) */
  readonly halfDayHours = computed((): number | null => {
    if (!this.isSingleDay()) return null;
    const { startTime, endTime } = this.formValues();
    if (!startTime || !endTime) return null;
    const [sh, sm] = startTime.split(':').map(Number);
    const [eh, em] = endTime.split(':').map(Number);
    const minutes = (eh * 60 + em) - (sh * 60 + sm);
    return minutes > 0 ? minutes / 60 : null;
  });

  /** Validation error for the time range (single-day only) */
  readonly timeRangeError = computed((): string | null => {
    if (!this.isSingleDay()) return null;
    const { startTime, endTime } = this.formValues();
    if (!startTime || !endTime) return null;
    const hours = this.halfDayHours();
    if (hours === null || hours <= 0) return "L'heure de fin doit être après l'heure de début.";
    if (hours > 8) return "La durée ne peut pas dépasser 8 heures.";
    return null;
  });

  readonly estimatedWorkingDays = computed((): number | null => {
    const { startDate, endDate } = this.formValues();
    if (!startDate || !endDate || endDate < startDate) return null;

    let workingDays = 0;
    const cur = new Date(startDate + 'T00:00:00');
    const end = new Date(endDate + 'T00:00:00');
    while (cur <= end) {
      const dow = cur.getDay();
      if (dow !== 0 && dow !== 6) workingDays++;
      cur.setDate(cur.getDate() + 1);
    }

    // Single day with a half-day time range (≤ 4h) → 0.5
    if (workingDays === 1 && this.isSingleDay()) {
      const hours = this.halfDayHours();
      if (hours !== null && hours <= 4) return 0.5;
    }

    return workingDays;
  });

  /** Days that will actually be deducted from the balance, applying leave-type rules */
  readonly deductibleDays = computed((): number | null => {
    const est = this.estimatedWorkingDays();
    if (est === null) return null;
    const typeCode = this.formValues().leaveTypeCode;
    const leaveType = typeCode ? this.leaveTypes().find(t => t.code === typeCode) ?? null : null;
    if (!leaveType) return est;
    switch (leaveType.deductionRule) {
      case 'NONE':        return 0;
      case 'EXCESS_ONLY': return Math.max(0, est - leaveType.allocatedDays);
      case 'FULL':        return est;
    }
  });

  /** True when deductible days exceed available balance */
  readonly insufficientBalance = computed(() => {
    const deductible = this.deductibleDays();
    const bal = this.selectedBalance();
    if (deductible === null || deductible === 0 || !bal) return false;
    return deductible > bal.available;
  });

  /** Validation error for the date range */
  readonly dateRangeError = computed((): string | null => {
    const { startDate, endDate } = this.formValues();
    if (!startDate) return null;
    if (endDate && endDate < startDate) return 'La date de fin doit être égale ou postérieure à la date de début.';
    return null;
  });

  /** True when the date range contains zero working days */
  readonly noWorkingDaysWarning = computed(() => this.estimatedWorkingDays() === 0);


  ngOnInit(): void {
    this.userService.getCurrentUser().subscribe({
      next: (user) => {
        this.currentUserId.set(user.id);
        this.loadData(user.id);
        this.loadColleagues();
      },
      error: () => this.loading.set(false),
    });
  }

  private loadColleagues(): void {
    this.leaveService.getColleagues().subscribe({
      next: (colleagues) => this.allColleagues.set(colleagues),
      error: () => {},
    });
  }

  private loadData(userId: number): void {
    this.loading.set(true);
    forkJoin({
      types: this.leaveService.getLeaveTypes(),
      balances: this.leaveService.getUserLeaveBalance(userId),
      requests: this.leaveService.getUserLeaveRequests(userId),
      emp: this.employeeService.getByUserIdOptional(userId),
      userBalance: this.leaveBalanceService.getBalanceForUser(),
    }).subscribe({
      next: ({ types, balances, requests, emp, userBalance }) => {
        this.leaveTypes.set(types);
        this.balances.set(balances);
        this.requests.set(requests);
        this.employeeInfo.set(emp);
        this.userBalance.set(userBalance);
        this.currentPage.set(1);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  toggleForm(): void {
    this.showForm.update((v) => !v);
    if (!this.showForm()) {
      this.form.reset({ startTime: '', endTime: '' });
      this.errorMsg.set(null);
      this.editingRequest.set(null);
      this.selectedApproverId.set(null);
      this.approverSearch.set('');
      this.showApproverDropdown.set(false);
      this.selectedNotifyIds.set(new Set());
      this.notifySearch.set('');
      this.showNotifyDropdown.set(false);
      this.attachedFiles.set([]);
      this.attachFileError.set(null);
      this.existingAttachments.set([]);
      this.removedAttachmentIds.set(new Set());
    }
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const files = input.files ? Array.from(input.files) : [];
    this.attachFileError.set(null);
    if (files.length === 0) return;

    const errors: string[] = [];
    const accepted: File[] = [];
    for (const file of files) {
      if (file.size > MyLeaveComponent.MAX_FILE_SIZE) {
        errors.push(`${file.name} : dépasse la taille maximale autorisée (10 Mo).`);
        continue;
      }
      if (!MyLeaveComponent.ALLOWED_TYPES.includes(file.type)) {
        errors.push(`${file.name} : format non autorisé (PDF, JPEG ou PNG uniquement).`);
        continue;
      }
      accepted.push(file);
    }
    if (accepted.length) {
      this.attachedFiles.update((list) => [...list, ...accepted]);
    }
    if (errors.length) {
      this.attachFileError.set(errors.join(' '));
    }
    input.value = '';
  }

  removeStagedFile(index: number): void {
    this.attachedFiles.update((list) => list.filter((_, i) => i !== index));
  }

  removeExistingAttachment(attachId: number): void {
    this.removedAttachmentIds.update((set) => {
      const next = new Set(set);
      next.add(attachId);
      return next;
    });
  }

  formatFileSize(bytes: number): string {
    if (bytes < 1024) return bytes + ' o';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' Ko';
    return (bytes / (1024 * 1024)).toFixed(1) + ' Mo';
  }

  toggleApproverDropdown(): void {
    this.showApproverDropdown.update((v) => !v);
    if (this.showApproverDropdown()) {
      this.approverSearch.set('');
    }
  }

  toggleApprover(id: number): void {
    this.selectedApproverId.set(id);
    this.showApproverDropdown.set(false);
  }

  closeApproverDropdown(): void {
    this.showApproverDropdown.set(false);
  }

  toggleNotifyDropdown(): void {
    this.showNotifyDropdown.update((v) => !v);
    if (this.showNotifyDropdown()) this.notifySearch.set('');
  }

  toggleNotify(id: number): void {
    this.selectedNotifyIds.update((set) => {
      const next = new Set(set);
      if (next.has(id)) next.delete(id); else next.add(id);
      return next;
    });
  }

  removeNotify(id: number): void {
    this.selectedNotifyIds.update((set) => {
      const next = new Set(set);
      next.delete(id);
      return next;
    });
  }

  closeNotifyDropdown(): void {
    this.showNotifyDropdown.set(false);
  }

  toggleExpand(requestId: number): void {
    this.expandedRequestId.update((id) => (id === requestId ? null : requestId));
  }

  onSubmit(): void {
    if (this.form.invalid || this.submitting() || this.dateRangeError() || this.timeRangeError() || this.noWorkingDaysWarning()) return;
    const userId = this.currentUserId();
    if (!userId) return;

    const v = this.form.value;
    const payload: LeaveApplicationRequest = {
      leaveTypeCode: v.leaveTypeCode!,
      startDate: v.startDate!,
      endDate: v.endDate!,
      comment: v.comment || undefined,
    };

    if (this.isSingleDay() && v.startTime && v.endTime) {
      payload.startTime = v.startTime;
      payload.endTime = v.endTime;
    }

    const approverId = this.selectedApproverId();
    if (!approverId) { this.errorMsg.set('Veuillez sélectionner un approbateur.'); return; }
    payload.approverUserIds = [approverId];

    const notifyIds = [...this.selectedNotifyIds()];
    if (notifyIds.length > 0) {
      payload.notifyUserIds = notifyIds;
    }

    this.submitting.set(true);
    this.errorMsg.set(null);

    const editing = this.editingRequest();

    const resetForm = () => {
      this.submitting.set(false);
      this.showForm.set(false);
      this.editingRequest.set(null);
      this.form.reset();
      this.selectedApproverId.set(null);
      this.approverSearch.set('');
      this.selectedNotifyIds.set(new Set());
      this.notifySearch.set('');
      this.attachedFiles.set([]);
      this.attachFileError.set(null);
      this.existingAttachments.set([]);
      this.removedAttachmentIds.set(new Set());
      this.loadData(userId);
    };

    const uploadStagedFiles = (leaveRequestId: number) =>
      from(this.attachedFiles()).pipe(
        concatMap((file) =>
          this.attachmentService.uploadLeaveRequestAttachment(leaveRequestId, file).pipe(catchError(() => of(null)))
        ),
        toArray()
      );

    const finish = (res: LeaveApplicationResponse, actionLabel: string, hadAttachmentIssue: boolean) => {
      let msg = `Demande ${actionLabel} — ${res.daysRequested} jour(s) en attente. Solde restant : ${res.balanceAfter} j.`;
      if (hadAttachmentIssue) {
        msg += " (Certaines pièces jointes n'ont pas pu être mises à jour — vérifiez la demande.)";
      }
      this.successMsg.set(msg);
      resetForm();
    };

    if (editing) {
      const oldId = editing.id;
      const kept = this.existingAttachments().filter((a) => !this.removedAttachmentIds().has(a.attachId));
      const removed = this.existingAttachments().filter((a) => this.removedAttachmentIds().has(a.attachId));

      this.leaveService.applyForLeave(userId, payload).subscribe({
        next: (res) => {
          const newId = res.leaveRequestId;
          const reconcileCalls = [
            ...kept.map((a) =>
              this.attachmentService.reassignLeaveRequestAttachment(newId, a.attachId).pipe(catchError(() => of(null)))
            ),
            ...removed.map((a) =>
              this.attachmentService.deleteLeaveRequestAttachment(oldId, a.attachId).pipe(catchError(() => of(null)))
            ),
          ];
          const reconcile$ = reconcileCalls.length ? forkJoin(reconcileCalls) : of([]);

          reconcile$.subscribe((reconcileResults) => {
            const reconcileFailed = reconcileResults.some((r) => r === null);
            this.leaveService.deleteLeaveRequest(oldId, userId).subscribe({
              next: () => {
                uploadStagedFiles(newId).subscribe((uploadResults) => {
                  const uploadFailed = uploadResults.some((r) => r === null);
                  finish(res, 'modifiée', reconcileFailed || uploadFailed);
                });
              },
              error: () => {
                this.errorMsg.set(
                  "La demande modifiée a été créée mais l'ancienne demande n'a pas pu être supprimée. Contactez le support si un doublon persiste."
                );
                this.submitting.set(false);
              },
            });
          });
        },
        error: (err) => {
          this.errorMsg.set(err?.error?.message ?? 'Impossible de modifier la demande.');
          this.submitting.set(false);
        },
      });
    } else {
      this.leaveService.applyForLeave(userId, payload).subscribe({
        next: (res) => {
          uploadStagedFiles(res.leaveRequestId).subscribe((uploadResults) => {
            finish(res, 'soumise', uploadResults.some((r) => r === null));
          });
        },
        error: (err) => {
          this.errorMsg.set(err?.error?.message ?? 'Une erreur est survenue.');
          this.submitting.set(false);
        },
      });
    }
  }

  startEdit(req: LeaveRequestSummary): void {
    const ownerComment = req.comments?.[0]?.text ?? '';

    this.editingRequest.set(req);
    this.form.patchValue({
      leaveTypeCode: req.leaveTypeCode,
      startDate: req.startDate,
      endDate: req.endDate,
      comment: ownerComment,
    });

    this.selectedApproverId.set(req.approvers?.[0]?.id ?? null);

    this.showForm.set(true);
    this.errorMsg.set(null);
    this.successMsg.set(null);
    this.attachedFiles.set([]);
    this.attachFileError.set(null);
    this.removedAttachmentIds.set(new Set());
    this.existingAttachments.set([]);
    this.loadingExistingAttachments.set(true);
    this.attachmentService.listLeaveRequestAttachments(req.id).subscribe({
      next: (atts) => {
        this.existingAttachments.set(atts);
        this.loadingExistingAttachments.set(false);
      },
      error: () => this.loadingExistingAttachments.set(false),
    });
  }

  deleteRequest(requestId: number): void {
    if (!confirm('Supprimer définitivement cette demande de congé ? Cette action est irréversible.')) return;
    const userId = this.currentUserId();
    if (!userId) return;
    this.leaveService.deleteLeaveRequest(requestId, userId).subscribe({
      next: () => this.loadData(userId),
      error: (err) =>
        this.errorMsg.set(err?.error?.message ?? 'Impossible de supprimer la demande.'),
    });
  }

  cancelRequest(requestId: number): void {
    const userId = this.currentUserId();
    if (!userId) return;
    this.leaveService.cancelLeaveRequest(requestId, userId).subscribe({
      next: () => this.loadData(userId),
      error: (err) =>
        this.errorMsg.set(err?.error?.message ?? 'Impossible d\'annuler la demande.'),
    });
  }

  statusClass(status: string): string {
    switch (status) {
      case 'En attente': return 'bg-amber-100 text-amber-800';
      case 'Approuvé':   return 'bg-blue-100 text-blue-800';
      case 'Pris':       return 'bg-green-100 text-green-800';
      case 'Rejeté':     return 'bg-red-100 text-red-800';
      case 'Annulé':     return 'bg-gray-100 text-gray-500';
      default:           return 'bg-gray-100 text-gray-500';
    }
  }

  formatDate(d: string): string {
    if (!d) return '';
    return new Date(d).toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    });
  }
}

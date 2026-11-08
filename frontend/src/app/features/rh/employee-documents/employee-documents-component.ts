import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { AttachmentService, AttachmentDTO, DOCUMENT_TYPES } from '../services/attachment.service';
import { NotificationService } from '../../../core/services/impl/notification.service';

@Component({
  selector: 'app-employee-documents',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './employee-documents-component.html',
})
export class EmployeeDocumentsComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly attachmentService = inject(AttachmentService);
  private readonly notificationService = inject(NotificationService);

  userId = signal<number | null>(null);
  attachments = signal<AttachmentDTO[]>([]);
  loading = signal(true);
  uploading = signal(false);
  showUploadModal = signal(false);
  uploadError = signal<string | null>(null);
  selectedFile = signal<File | null>(null);
  uploadDocType = signal('bulletin-salaire');
  uploadDescription = signal('Bulletin de salaire');

  readonly documentTypes = DOCUMENT_TYPES;

  ngOnInit(): void {
    const userIdParam = this.route.parent?.snapshot.paramMap.get('userId');
    const userId = userIdParam ? parseInt(userIdParam, 10) : null;
    if (!userId || isNaN(userId)) {
      this.loading.set(false);
      return;
    }

    this.userId.set(userId);
    this.loadAttachments(userId);
  }

  private loadAttachments(userId: number): void {
    this.loading.set(true);
    this.attachmentService.list(userId).subscribe({
      next: (list) => { this.attachments.set(list); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  openUploadModal(): void {
    this.selectedFile.set(null);
    this.uploadDocType.set('bulletin-salaire');
    this.uploadDescription.set('Bulletin de salaire');
    this.uploadError.set(null);
    this.showUploadModal.set(true);
  }

  closeUploadModal(): void {
    this.showUploadModal.set(false);
  }

  onFileSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0] ?? null;
    this.selectedFile.set(file);
  }

  onDocTypeChange(event: Event): void {
    const val = (event.target as HTMLSelectElement).value;
    const found = this.documentTypes.find((t) => t.value === val);
    if (found) {
      this.uploadDocType.set(found.value);
      this.uploadDescription.set(found.label);
    }
  }

  confirmUpload(): void {
    const file = this.selectedFile();
    const userId = this.userId();
    if (!file || !userId) return;

    this.uploading.set(true);
    this.uploadError.set(null);
    this.attachmentService.upload(userId, file, this.uploadDescription(), this.uploadDocType()).subscribe({
      next: (dto) => {
        this.attachments.update((list) => [dto, ...list]);
        this.uploading.set(false);
        this.showUploadModal.set(false);
        this.notificationService.success('Document ajouté', `"${dto.filename}" a été ajouté avec succès.`);
      },
      error: (err) => {
        this.uploading.set(false);
        this.uploadError.set(err?.error?.message ?? "Erreur lors de l'envoi du fichier.");
      },
    });
  }

  downloadAttachment(userId: number, attachId: number, filename: string): void {
    this.attachmentService.download(userId, attachId).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => this.notificationService.error('Erreur', 'Impossible de télécharger le document.'),
    });
  }

  deleteAttachment(userId: number, attachId: number): void {
    this.attachmentService.delete(userId, attachId).subscribe({
      next: () => {
        this.attachments.update((list) => list.filter((a) => a.attachId !== attachId));
        this.notificationService.success('Document supprimé', 'Le document a été supprimé.');
      },
      error: () => this.notificationService.error('Erreur', 'Impossible de supprimer le document.'),
    });
  }

  formatFileSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} o`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} Ko`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} Mo`;
  }

  formatDate(value: string | undefined): string {
    if (!value) return '—';
    const d = value.split('T')[0];
    const [y, m, day] = d.split('-');
    return `${day}/${m}/${y}`;
  }

  getDocTypeLabel(screen: string): string {
    return this.documentTypes.find((t) => t.value === screen)?.label ?? screen;
  }
}

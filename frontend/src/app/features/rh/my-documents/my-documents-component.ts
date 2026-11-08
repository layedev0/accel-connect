import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AttachmentDTO, AttachmentService, DOCUMENT_TYPES } from '../services/attachment.service';

@Component({
  selector: 'app-my-documents',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './my-documents-component.html',
})
export class MyDocumentsComponent implements OnInit {
  private readonly attachmentService = inject(AttachmentService);

  loading = signal(true);
  error = signal<string | null>(null);
  attachments = signal<AttachmentDTO[]>([]);
  downloading = signal<number | null>(null);

  readonly pageSize = 10;
  readonly currentPage = signal(1);
  readonly totalPages = computed(() => Math.ceil(this.attachments().length / this.pageSize));
  readonly pageNumbers = computed(() => Array.from({ length: this.totalPages() }, (_, i) => i + 1));
  readonly paginatedDocs = computed(() => {
    const start = (this.currentPage() - 1) * this.pageSize;
    return this.attachments().slice(start, start + this.pageSize);
  });
  pageEnd(): number {
    return Math.min(this.currentPage() * this.pageSize, this.attachments().length);
  }

  ngOnInit(): void {
    this.attachmentService.listOwn().subscribe({
      next: (list) => {
        this.attachments.set(list);
        this.currentPage.set(1);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'Impossible de charger vos documents.');
        this.loading.set(false);
      },
    });
  }

  download(attachment: AttachmentDTO): void {
    this.downloading.set(attachment.attachId);
    this.attachmentService.downloadOwn(attachment.attachId).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = attachment.filename;
        a.click();
        URL.revokeObjectURL(url);
        this.downloading.set(null);
      },
      error: () => {
        this.downloading.set(null);
      },
    });
  }

  getDocTypeLabel(screen: string): string {
    return DOCUMENT_TYPES.find((t) => t.value === screen)?.label ?? screen;
  }

  formatFileSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} o`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} Ko`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} Mo`;
  }

  formatDate(value: string): string {
    if (!value) return '—';
    const d = new Date(value);
    return d.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric' });
  }
}

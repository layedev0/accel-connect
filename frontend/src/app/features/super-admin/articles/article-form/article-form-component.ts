import { Component, EventEmitter, Input, OnDestroy, OnInit, Output, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EMPTY, catchError, concatMap, from } from 'rxjs';
import {
  ArticleService,
  ArticleDTO,
  ArticlePhotoDTO,
  ArticleType,
  ARTICLE_TYPES,
} from '../../../home/services/article.service';
import { NotificationService } from '../../../../core/services/impl/notification.service';

/** A photo chosen but not yet uploaded, with its local preview URL. */
interface PendingPhoto {
  file: File;
  previewUrl: string;
}

/** A photo already stored server-side, with the blob URL used to preview it. */
interface ExistingPhoto {
  id: number;
  fileName: string;
  url: string;
  position: number;
}

@Component({
  selector: 'app-article-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './article-form-component.html',
})
export class ArticleFormComponent implements OnInit, OnDestroy {
  private readonly articleService = inject(ArticleService);
  private readonly notificationService = inject(NotificationService);

  /** Server-side cap; mirrored here so the picker can stop before the request fails. */
  readonly maxPhotos = 10;

  /** When set, the form is in EDIT mode (fields are prefilled; the cover image is optional). */
  @Input() article: ArticleDTO | null = null;

  /** Emitted with the newly created article so the parent can refresh its list. */
  @Output() created = new EventEmitter<ArticleDTO>();
  /** Emitted with the updated article after a successful edit. */
  @Output() updated = new EventEmitter<ArticleDTO>();
  @Output() closed = new EventEmitter<void>();

  readonly articleTypes = ARTICLE_TYPES;

  title = signal('');
  content = signal('');
  articleType = signal<ArticleType>('FORMATION');
  selectedCover = signal<File | null>(null);
  pendingPhotos = signal<PendingPhoto[]>([]);
  existingPhotos = signal<ExistingPhoto[]>([]);
  submitting = signal(false);
  error = signal<string | null>(null);

  get isEditMode(): boolean {
    return this.article !== null;
  }

  remainingPhotoSlots(): number {
    return Math.max(0, this.maxPhotos - this.existingPhotos().length - this.pendingPhotos().length);
  }

  ngOnInit(): void {
    if (this.article) {
      this.title.set(this.article.title);
      this.content.set(this.article.content);
      this.articleType.set(this.article.articleType);
      this.loadExistingPhotos(this.article.id);
    }
  }

  ngOnDestroy(): void {
    for (const pending of this.pendingPhotos()) URL.revokeObjectURL(pending.previewUrl);
    for (const photo of this.existingPhotos()) URL.revokeObjectURL(photo.url);
  }

  onCoverSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0] ?? null;
    this.selectedCover.set(file);
  }

  onPhotosSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const chosen = Array.from(input.files ?? []);
    if (chosen.length === 0) return;

    const accepted = chosen.slice(0, this.remainingPhotoSlots());
    if (accepted.length < chosen.length) {
      this.notificationService.warning(
        'Trop de photos',
        `Un article accepte au maximum ${this.maxPhotos} photos.`,
      );
    }

    this.pendingPhotos.update((list) => [
      ...list,
      ...accepted.map((file) => ({ file, previewUrl: URL.createObjectURL(file) })),
    ]);

    // Let the same file be picked again after being removed.
    input.value = '';
  }

  removePendingPhoto(pending: PendingPhoto): void {
    URL.revokeObjectURL(pending.previewUrl);
    this.pendingPhotos.update((list) => list.filter((item) => item !== pending));
  }

  /** Deletion is immediate, as everywhere else in the back-office — not deferred to save. */
  removeExistingPhoto(photo: ExistingPhoto): void {
    if (!this.article) return;

    this.articleService.deletePhoto(this.article.id, photo.id).subscribe({
      next: () => {
        URL.revokeObjectURL(photo.url);
        this.existingPhotos.update((list) => list.filter((item) => item.id !== photo.id));
      },
      error: () =>
        this.notificationService.error('Suppression impossible', "La photo n'a pas pu être supprimée."),
    });
  }

  formatFileSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} o`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} Ko`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} Mo`;
  }

  close(): void {
    this.closed.emit();
  }

  submit(): void {
    const title = this.title().trim();
    const content = this.content().trim();

    if (!title || !content) {
      this.error.set('Veuillez renseigner le titre et le contenu.');
      return;
    }

    this.submitting.set(true);
    this.error.set(null);

    if (this.isEditMode && this.article) {
      this.articleService
        .update(this.article.id, title, content, this.articleType(), this.selectedCover())
        .subscribe({
          next: (dto) => this.uploadPendingPhotos(dto, 'updated'),
          error: (err) => {
            this.submitting.set(false);
            this.error.set(err?.error?.message ?? "Erreur lors de la mise à jour de l'article.");
          },
        });
      return;
    }

    this.articleService.create(title, content, this.articleType(), this.selectedCover()).subscribe({
      next: (dto) => this.uploadPendingPhotos(dto, 'created'),
      error: (err) => {
        this.submitting.set(false);
        this.error.set(err?.error?.message ?? "Erreur lors de la publication de l'article.");
      },
    });
  }

  /**
   * Photos go up one request at a time: the server caps a whole multipart request at 10 MB,
   * so batching them would fail well before the per-file limit.
   */
  private uploadPendingPhotos(dto: ArticleDTO, outcome: 'created' | 'updated'): void {
    const pending = this.pendingPhotos();

    if (pending.length === 0) {
      this.finish(dto, outcome);
      return;
    }

    const failed: string[] = [];

    from(pending)
      .pipe(
        concatMap((photo) =>
          this.articleService.uploadPhoto(dto.id, photo.file).pipe(
            // One failure must not abort the photos queued behind it.
            catchError(() => {
              failed.push(photo.file.name);
              return EMPTY;
            }),
          ),
        ),
      )
      .subscribe({ complete: () => this.finishWithPhotoErrors(dto, outcome, failed) });
  }

  private finishWithPhotoErrors(dto: ArticleDTO, outcome: 'created' | 'updated', failed: string[]): void {
    if (failed.length > 0) {
      this.notificationService.error(
        'Photos non envoyées',
        `${failed.join(', ')} n'a pas pu être envoyée. L'article est enregistré, réessayez depuis la modification.`,
      );
    }
    this.finish(dto, outcome);
  }

  private finish(dto: ArticleDTO, outcome: 'created' | 'updated'): void {
    this.submitting.set(false);

    if (outcome === 'updated') {
      this.notificationService.success('Article modifié', `"${dto.title}" a été mis à jour.`);
      this.updated.emit(dto);
      return;
    }

    this.notificationService.success('Article publié', `"${dto.title}" a été ajouté avec succès.`);
    this.created.emit(dto);
  }

  private loadExistingPhotos(articleId: number): void {
    this.articleService.getPhotos(articleId).subscribe({
      next: (list) => list.forEach((meta) => this.loadExistingPhoto(articleId, meta)),
      error: (err) => console.warn('⚠️ Failed to load article photos:', err),
    });
  }

  private loadExistingPhoto(articleId: number, meta: ArticlePhotoDTO): void {
    this.articleService.downloadPhoto(articleId, meta.id).subscribe({
      next: (blob) => {
        // The blobs land out of order, so re-sort on position after each arrival.
        this.existingPhotos.update((list) =>
          [
            ...list,
            { id: meta.id, fileName: meta.fileName, url: URL.createObjectURL(blob), position: meta.position },
          ].sort((a, b) => a.position - b.position),
        );
      },
      error: (err) => console.warn('⚠️ Failed to load article photo:', err),
    });
  }
}

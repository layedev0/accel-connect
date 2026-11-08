import { formatDate, isPlatformBrowser } from '@angular/common';
import {
  Component,
  ElementRef,
  LOCALE_ID,
  OnDestroy,
  OnInit,
  PLATFORM_ID,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ConfirmationService } from '../../../core/services/impl/ConfirmationDialog.service';
import { NotificationService } from '../../../core/services/impl/notification.service';
import { Article } from '../models/article.model';
import {
  ArticleGalleryComponent,
  GalleryPhoto,
} from './article-gallery/article-gallery-component';
import {
  ArticleCommentDTO,
  ArticleDTO,
  ArticleLikeStatusDTO,
  ArticleService,
  ArticleType,
} from '../services/article.service';

/** A comment plus the two fields the template needs, computed once instead of per render. */
interface CommentView extends ArticleCommentDTO {
  authorInitials: string;
  when: string;
}

@Component({
  selector: 'app-article-detail',
  standalone: true,
  imports: [ArticleGalleryComponent],
  templateUrl: './article-detail-component.html',
  styleUrl: './article-detail-component.css',
})
export class ArticleDetailComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly articleService = inject(ArticleService);
  private readonly isBrowser = isPlatformBrowser(inject(PLATFORM_ID));
  private readonly locale = inject(LOCALE_ID);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly notificationService = inject(NotificationService);

  // Declared after `locale`: field initialisers run in order.
  private readonly relativeTime = new Intl.RelativeTimeFormat(this.locale, { numeric: 'auto' });

  private coverObjectUrl: string | null = null;
  private photoObjectUrls: string[] = [];

  private readonly bodyRef = viewChild<ElementRef<HTMLElement>>('articleBody');

  /** The article's numeric id. `article().id` is a string, and `documentId` is misleadingly named. */
  private readonly articleId = signal<number | null>(null);

  readonly article = signal<Article | null>(null);
  readonly loading = signal(true);
  readonly notFound = signal(false);
  readonly expanded = signal(false);

  readonly photos = signal<GalleryPhoto[]>([]);
  readonly photosLoading = signal(false);

  readonly comments = signal<CommentView[]>([]);
  readonly commentsLoading = signal(false);
  readonly commentsExpanded = signal(false);
  readonly likeStatus = signal<ArticleLikeStatusDTO>({ likeCount: 0, likedByCurrentUser: false });
  readonly draft = signal('');
  readonly posting = signal(false);
  readonly likePending = signal(false);

  readonly contentParagraphs = computed(() =>
    (this.article()?.description ?? '')
      .split(/\n+/)
      .map((p) => p.trim())
      .filter(Boolean),
  );

  /**
   * Long articles open on a preview capped at 32rem, behind a "Voir plus" toggle.
   * The threshold is measured on the content rather than on the rendered height: the route
   * is server-rendered, so a DOM measurement would shift the layout on hydration.
   */
  readonly isCollapsible = computed(
    () => (this.article()?.description ?? '').length > ArticleDetailComponent.COLLAPSE_THRESHOLD,
  );

  /** Roughly the number of characters that fills the 32rem preview at 17px/1.8. */
  private static readonly COLLAPSE_THRESHOLD = 1500;

  /** Comments shown before the thread has to be unfolded. */
  private static readonly COMMENTS_PREVIEW = 3;

  readonly visibleComments = computed(() =>
    this.commentsExpanded()
      ? this.comments()
      : this.comments().slice(0, ArticleDetailComponent.COMMENTS_PREVIEW),
  );

  readonly hiddenCommentCount = computed(() =>
    Math.max(0, this.comments().length - ArticleDetailComponent.COMMENTS_PREVIEW),
  );

  private static readonly ARTICLE_CATEGORIES: Record<ArticleType, { label: string; color: string }> = {
    FORMATION: { label: 'Formation', color: '#25509D' },
    RECHERCHE: { label: 'Recherche', color: '#25509D' },
    BUSINESS: { label: 'Business', color: '#99CFBD' },
    PARTENARIAT: { label: 'Partenariat', color: '#E84141' },
  };

  ngOnInit(): void {
    const articleId = this.route.snapshot.paramMap.get('id');
    if (!articleId) {
      this.notFound.set(true);
      this.loading.set(false);
      return;
    }

    this.articleService.list().subscribe({
      next: (articles) => {
        const dto = articles.find((item) => String(item.id) === articleId);
        if (!dto) {
          this.notFound.set(true);
          this.loading.set(false);
          return;
        }

        this.articleId.set(dto.id);
        this.article.set(this.toDisplayArticle(dto));
        this.loading.set(false);

        if (!this.isBrowser) return;
        if (dto.hasCoverImage) this.loadCover(dto.id);
        this.loadPhotos(dto.id, dto.title);
        this.loadComments(dto.id);
        this.loadLikeStatus(dto.id);
      },
      error: (err) => {
        console.warn('⚠️ Failed to load article detail:', err);
        this.notFound.set(true);
        this.loading.set(false);
      },
    });
  }

  goBack(): void {
    void this.router.navigate(['/']);
  }

  toggleExpanded(): void {
    const next = !this.expanded();
    this.expanded.set(next);

    // Collapsing from the bottom of a long article would leave the reader below the fold.
    if (!next && this.isBrowser) {
      this.bodyRef()?.nativeElement.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }

  /** Background tint for the category chip: the category colour at 10% opacity. */
  categoryTint(article: Article): string {
    return `${article.categoryColor}1A`;
  }

  sharePdf(article: Article): void {
    if (!this.isBrowser) return;
    if (navigator.share) {
      navigator
        .share({
          title: article.title,
          text: article.subtitle,
          url: window.location.href,
        })
        .catch((err) => console.log('Erreur de partage:', err));
      return;
    }

    navigator.clipboard.writeText(window.location.href);
    alert('Lien copié dans le presse-papier!');
  }

  /** No scroll-back on collapse, unlike the article body: the thread sits at the page bottom. */
  toggleComments(): void {
    this.commentsExpanded.update((expanded) => !expanded);
  }

  toggleLike(): void {
    const id = this.articleId();
    if (id == null || this.likePending()) return;

    this.likePending.set(true);
    const request = this.likeStatus().likedByCurrentUser
      ? this.articleService.unlike(id)
      : this.articleService.like(id);

    request.subscribe({
      next: (status) => {
        this.likeStatus.set(status);
        this.likePending.set(false);
      },
      error: () => {
        this.likePending.set(false);
        this.notificationService.error(
          'Action impossible',
          "Votre « j'aime » n'a pas pu être enregistré. Réessayez.",
        );
      },
    });
  }

  postComment(): void {
    const id = this.articleId();
    const content = this.draft().trim();
    if (id == null || !content || this.posting()) return;

    this.posting.set(true);
    this.articleService.addComment(id, content).subscribe({
      next: (created) => {
        // The list is newest-first, so the new comment goes on top.
        this.comments.update((list) => [this.toCommentView(created), ...list]);
        this.draft.set('');
        this.posting.set(false);
      },
      error: () => {
        this.posting.set(false);
        this.notificationService.error(
          'Publication impossible',
          "Votre commentaire n'a pas pu être publié. Réessayez.",
        );
      },
    });
  }

  confirmDeleteComment(comment: CommentView): void {
    const id = this.articleId();
    if (id == null) return;

    this.confirmationService.confirmDelete(
      'Supprimer ce commentaire ?',
      'Cette action est irréversible.',
      () => {
        this.articleService.deleteComment(id, comment.id).subscribe({
          next: () => {
            this.comments.update((list) => list.filter((item) => item.id !== comment.id));
            this.notificationService.success('Commentaire supprimé', 'Il n\'apparaît plus sur l\'article.');
          },
          error: () =>
            this.notificationService.error(
              'Suppression impossible',
              "Le commentaire n'a pas pu être supprimé.",
            ),
        });
      },
    );
  }

  ngOnDestroy(): void {
    this.revokeObjectUrl(this.coverObjectUrl);
    this.coverObjectUrl = null;
    for (const url of this.photoObjectUrls) this.revokeObjectUrl(url);
    this.photoObjectUrls = [];
  }

  /**
   * The bytes are token-protected, so each photo is fetched as a blob and turned into an
   * object URL. The metadata call returns them already ordered by position.
   */
  private loadPhotos(articleId: number, articleTitle: string): void {
    this.photosLoading.set(true);

    this.articleService.getPhotos(articleId).subscribe({
      next: (list) => {
        if (list.length === 0) {
          this.photosLoading.set(false);
          return;
        }

        // Placeholders keep the server's order while the blobs land out of sequence.
        const slots: (GalleryPhoto | null)[] = list.map(() => null);
        let remaining = list.length;

        list.forEach((meta, index) => {
          this.articleService.downloadPhoto(articleId, meta.id).subscribe({
            next: (blob) => {
              const url = URL.createObjectURL(blob);
              this.photoObjectUrls.push(url);
              slots[index] = {
                id: meta.id,
                url,
                alt: `${articleTitle} — photo ${index + 1}`,
              };
              this.settlePhoto(slots, --remaining);
            },
            error: (err) => {
              console.warn('⚠️ Failed to load article photo:', err);
              this.settlePhoto(slots, --remaining);
            },
          });
        });
      },
      error: (err) => {
        console.warn('⚠️ Failed to load article photos:', err);
        this.photosLoading.set(false);
      },
    });
  }

  /** Publishes the gallery once every request has settled, dropping the ones that failed. */
  private settlePhoto(slots: (GalleryPhoto | null)[], remaining: number): void {
    if (remaining > 0) return;
    this.photos.set(slots.filter((photo): photo is GalleryPhoto => photo !== null));
    this.photosLoading.set(false);
  }

  private loadComments(articleId: number): void {
    this.commentsLoading.set(true);
    this.articleService.getComments(articleId).subscribe({
      next: (list) => {
        this.comments.set(list.map((dto) => this.toCommentView(dto)));
        this.commentsLoading.set(false);
      },
      error: (err) => {
        console.warn('⚠️ Failed to load article comments:', err);
        this.commentsLoading.set(false);
      },
    });
  }

  private loadLikeStatus(articleId: number): void {
    this.articleService.getLikeStatus(articleId).subscribe({
      next: (status) => this.likeStatus.set(status),
      error: (err) => console.warn('⚠️ Failed to load article likes:', err),
    });
  }

  private toCommentView(dto: ArticleCommentDTO): CommentView {
    return { ...dto, authorInitials: this.initials(dto.authorName), when: this.timeAgo(dto.createdAt) };
  }

  private initials(name: string): string {
    const parts = (name ?? '').trim().split(/\s+/).filter(Boolean);
    if (parts.length === 0) return '?';
    const letters = parts.length === 1 ? parts[0].slice(0, 2) : parts[0][0] + parts[parts.length - 1][0];
    return letters.toUpperCase();
  }

  /** "il y a 2 h" up to a week, then the absolute date. */
  private timeAgo(iso: string): string {
    const elapsedMs = Date.now() - new Date(iso).getTime();
    const minutes = Math.round(elapsedMs / 60_000);

    if (minutes < 1) return "à l'instant";
    if (minutes < 60) return this.relativeTime.format(-minutes, 'minute');

    const hours = Math.round(minutes / 60);
    if (hours < 24) return this.relativeTime.format(-hours, 'hour');

    const days = Math.round(hours / 24);
    if (days <= 7) return this.relativeTime.format(-days, 'day');

    return formatDate(iso, 'd MMMM y', this.locale);
  }

  private loadCover(id: number): void {
    this.articleService.downloadCover(id).subscribe({
      next: (blob) => {
        this.revokeObjectUrl(this.coverObjectUrl);
        this.coverObjectUrl = URL.createObjectURL(blob);
        this.article.update((current) => (current ? { ...current, coverImage: this.coverObjectUrl! } : current));
      },
      error: () => {
        /* leave the cover empty on failure: the template then skips the figure */
      },
    });
  }

  private revokeObjectUrl(url: string | null): void {
    if (url) URL.revokeObjectURL(url);
  }

  private toDisplayArticle(dto: ArticleDTO): Article {
    const category = ArticleDetailComponent.ARTICLE_CATEGORIES[dto.articleType] ?? {
      label: dto.articleType,
      color: '#25509D',
    };

    return {
      id: String(dto.id),
      documentId: dto.id,
      title: dto.title,
      subtitle: dto.content,
      category: category.label,
      categoryColor: category.color,
      // Left empty when the article has no cover: the template then skips the figure
      // rather than showing an unrelated placeholder photo.
      coverImage: '',
      publishedDate: this.formatPublishedDate(dto.createdAt),
      authorName: dto.authorName,
      readTime: this.estimateReadTime(dto.content),
      description: dto.content,
    };
  }

  /** Articles created before the created_at column existed have no date: the template omits it. */
  private formatPublishedDate(createdAt?: string): string {
    if (!createdAt) return '';
    return formatDate(createdAt, 'd MMMM y', this.locale);
  }

  private estimateReadTime(content: string): string {
    const words = content?.trim() ? content.trim().split(/\s+/).length : 0;
    return `${Math.max(1, Math.round(words / 200))} min`;
  }
}

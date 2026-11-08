import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ArticleService,
  ArticleDTO,
  ARTICLE_TYPES,
} from '../../home/services/article.service';
import { NotificationService } from '../../../core/services/impl/notification.service';
import { ConfirmationService } from '../../../core/services/impl/ConfirmationDialog.service';
import { ArticleFormComponent } from './article-form/article-form-component';

@Component({
  selector: 'app-article-management',
  standalone: true,
  imports: [CommonModule, ArticleFormComponent],
  templateUrl: './article-management-component.html',
  styleUrl: './article-management-component.css',
})
export class ArticleManagementComponent implements OnInit {
  private readonly articleService = inject(ArticleService);
  private readonly notificationService = inject(NotificationService);
  private readonly confirmationService = inject(ConfirmationService);

  articles = signal<ArticleDTO[]>([]);
  loading = signal(true);

  showFormModal = signal(false);
  /** null → create mode; an article → edit mode. */
  editingArticle = signal<ArticleDTO | null>(null);

  ngOnInit(): void {
    this.loadArticles();
  }

  private loadArticles(): void {
    this.loading.set(true);
    this.articleService.list().subscribe({
      next: (list) => {
        this.articles.set(list ?? []);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.notificationService.error('Erreur', 'Impossible de charger les articles.');
      },
    });
  }

  categoryLabel(type: string): string {
    return ARTICLE_TYPES.find((t) => t.value === type)?.label ?? type;
  }

  openCreateModal(): void {
    this.editingArticle.set(null);
    this.showFormModal.set(true);
  }

  openEditModal(article: ArticleDTO): void {
    this.editingArticle.set(article);
    this.showFormModal.set(true);
  }

  closeModal(): void {
    this.showFormModal.set(false);
    this.editingArticle.set(null);
  }

  onSaved(): void {
    this.closeModal();
    this.loadArticles();
  }

  confirmDelete(article: ArticleDTO): void {
    this.confirmationService.confirmDelete(
      'Supprimer cet article ?',
      `Êtes-vous sûr de vouloir supprimer "${article.title}" ? Cette action est irréversible.`,
      () => {
        this.articleService.delete(article.id).subscribe({
          next: () => {
            this.articles.update((list) => list.filter((a) => a.id !== article.id));
            this.notificationService.success('Article supprimé', `"${article.title}" a été supprimé.`);
          },
          error: () => this.notificationService.error('Erreur de suppression', 'Impossible de supprimer l\'article.'),
        });
      }
    );
  }
}

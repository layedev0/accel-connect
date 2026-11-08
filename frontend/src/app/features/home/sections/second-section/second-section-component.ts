import {
  Component,
  LOCALE_ID,
  OnDestroy,
  OnInit,
  PLATFORM_ID,
  computed,
  inject,
  signal,
} from '@angular/core';
import { CommonModule, formatDate, isPlatformBrowser } from '@angular/common';
import { Router } from '@angular/router';
import {
  EmployeeDetailsService,
  UpcomingBirthday,
} from '../../../rh/services/employee-details.service';
import { ArticleService, ArticleDTO, ArticleType } from '../../services/article.service';
import { Article } from '../../models/article.model';

export interface Birthday {
  id?: string;
  name: string;
  date: Date;
  avatar?: string;
}

/** A type/category bucket rendered as one horizontal carousel row. */
export interface ArticleGroup {
  label: string;
  color: string;
  items: Article[];
}

@Component({
  selector: 'app-second-section-component',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './second-section-component.html',
  styleUrl: './second-section-component.css',
})
export class SecondSectionComponent implements OnInit, OnDestroy {
  // Calendrier
  readonly now = new Date();
  year = this.now.getFullYear();
  month = this.now.getMonth() + 1;
  selectedDay: number | null = this.now.getDate();
  monthName = new Date(this.year, this.month - 1, 1).toLocaleString('fr-FR', { month: 'long' });
  readonly weekDays = ['DIM', 'LUN', 'MAR', 'MER', 'JEU', 'VEN', 'SAM'];
  calendarCells: Array<number | null> = [];

  // Anniversaires à venir
  private readonly employeeDetailsService = inject(EmployeeDetailsService);
  private readonly isBrowser = isPlatformBrowser(inject(PLATFORM_ID));
  private readonly locale = inject(LOCALE_ID);
  upcomingBirthdays = signal<UpcomingBirthday[]>([]);

  // Articles (actualités) chargés depuis le backend (lecture seule ici ;
  // la gestion se fait dans l'espace Super Admin)
  private readonly articleService = inject(ArticleService);
  private readonly router = inject(Router);

  /** Object URLs created for article cover images; revoked on destroy. */
  private coverObjectUrls: string[] = [];

  private static readonly ARTICLE_CATEGORIES: Record<ArticleType, { label: string; color: string }> = {
    FORMATION: { label: 'Formation', color: '#25509D' },
    RECHERCHE: { label: 'Recherche', color: '#25509D' },
    BUSINESS: { label: 'Business', color: '#99CFBD' },
    PARTENARIAT: { label: 'Partenariat', color: '#E84141' },
  };
  private static readonly DEFAULT_COVER =
    'https://images.unsplash.com/photo-1460925895917-afdab827c52f?w=600&q=80';
  private static readonly CATEGORY_ORDER = ['Recherche', 'Formation', 'Business', 'Partenariat'];

  /** Articles grouped by category label (ordered) — one carousel row per type. */
  readonly groupedArticles = computed<ArticleGroup[]>(() => {
    const groups = new Map<string, ArticleGroup>();
    for (const a of this.articles()) {
      let g = groups.get(a.category);
      if (!g) {
        g = { label: a.category, color: a.categoryColor, items: [] };
        groups.set(a.category, g);
      }
      g.items.push(a);
    }
    const order = SecondSectionComponent.CATEGORY_ORDER;
    const rank = (label: string) => {
      const i = order.indexOf(label);
      return i === -1 ? order.length : i;
    };
    return [...groups.values()].sort((x, y) => rank(x.label) - rank(y.label));
  });

  private readonly articleIndexes = signal<Record<string, number>>({});

  currentArticleForGroup(group: ArticleGroup): Article | null {
    if (!group.items.length) return null;
    const index = this.articleIndexes()[group.label] ?? 0;
    return group.items[index] ?? group.items[0];
  }

  cycleGroupArticle(group: ArticleGroup, dir: 1 | -1): void {
    if (group.items.length <= 1) return;
    this.articleIndexes.update((indexes) => {
      const current = indexes[group.label] ?? 0;
      const next = (current + dir + group.items.length) % group.items.length;
      return { ...indexes, [group.label]: next };
    });
  }

  /** Days of the currently displayed month that have a birthday → collaborator names. */
  private birthdaysByDay = new Map<number, string[]>();

  // Pagination (3 cards per page)
  readonly birthdaysPerPage = 3;
  birthdayPage = signal(1);

  private static readonly MONTHS_FR = [
    'janvier', 'février', 'mars', 'avril', 'mai', 'juin',
    'juillet', 'août', 'septembre', 'octobre', 'novembre', 'décembre',
  ];

  // Liste des articles avec PDF (signal → rendu fiable en change detection zoneless)
  articles = signal<Article[]>([
    {
      id: '1',
      title: 'Innover sans compromis : regards croisés',
      subtitle: 'Innover sans compromis: regards croisés',
      category: 'Recherche',
      categoryColor: '#25509D',
      coverImage: '/assets/img/mohamedxaicha.png',
      publishedDate: 'Il y a 2h',
      readTime: '12 min',
      description:
        "Une analyse approfondie des enjeux de sécurité dans l'innovation technologique.",
    },
    {
      id: '2',
      title: 'Nouveau programme de formation 2025',
      subtitle: 'Développez vos compétences',
      category: 'Formation',
      categoryColor: '#25509D',
      coverImage:
        'https://forbesafrique.com/wp-content/uploads/2025/01/Alioune-Mbaye-CEO-Accel-Tech-%C2%A9Accel-Tech-Par-AudysseyProd-Pour-Forbes-Afrique-Web-Jan.2025_Forbesafrique.com_.jpg',
      publishedDate: 'Il y a 2h',
      readTime: '5 min',
    },
    {
      id: '3',
      title: 'Résultats trimestriels exceptionnels',
      subtitle: 'Performance Q4 2024',
      category: 'Business',
      categoryColor: '#99CFBD',
      coverImage: 'https://images.unsplash.com/photo-1460925895917-afdab827c52f?w=600&q=80',
      publishedDate: 'Hier',
      readTime: '3 min',
    },
    {
      id: '4',
      title: 'Partenariat stratégique annoncé',
      subtitle: 'Nouvelle ère de collaboration',
      category: 'Hot',
      categoryColor: '#E84141',
      coverImage: 'https://images.unsplash.com/photo-1521737711867-e3b97375f902?w=600&q=80',
      publishedDate: 'Il y a 3j',
      readTime: '4 min',
    },
  ]);

  ngOnInit(): void {
    this.generateCalendar();
    this.loadUpcomingBirthdays();
    this.resetArticleIndexes();
    this.loadArticles();
  }

  private loadArticles(): void {
    // Skip the backend call during SSR; hydration re-runs this in the browser.
    if (!this.isBrowser) return;

    this.articleService.list().subscribe({
      next: (list) => {
        // Keep the built-in defaults when no article has been published yet.
        if (list && list.length > 0) {
          this.articles.set(list.map((dto) => this.toDisplayArticle(dto)));
          this.resetArticleIndexes();
          // Cover images are token-protected, so fetch each as a blob and swap it in.
          for (const dto of list) {
            if (dto.hasCoverImage) this.loadCover(dto.id);
          }
        }
      },
      error: (err) => console.warn('⚠️ Failed to load articles:', err),
    });
  }

  private loadCover(id: number): void {
    this.articleService.downloadCover(id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        this.coverObjectUrls.push(url);
        this.articles.update((arr) =>
          arr.map((a) => (a.documentId === id ? { ...a, coverImage: url } : a)),
        );
      },
      error: () => {
        /* keep the default cover on failure */
      },
    });
  }

  private toDisplayArticle(dto: ArticleDTO): Article {
    const category = SecondSectionComponent.ARTICLE_CATEGORIES[dto.articleType] ?? {
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
      coverImage: SecondSectionComponent.DEFAULT_COVER,
      publishedDate: this.formatPublishedDate(dto.createdAt),
      readTime: this.estimateReadTime(dto.content),
      description: dto.content,
    };
  }

  /** Articles created before the created_at column existed have no date: the card omits it. */
  private formatPublishedDate(createdAt?: string): string {
    if (!createdAt) return '';
    return formatDate(createdAt, 'd MMM y', this.locale);
  }

  private estimateReadTime(content: string): string {
    const words = content?.trim() ? content.trim().split(/\s+/).length : 0;
    return `${Math.max(1, Math.round(words / 200))} min`;
  }

  private resetArticleIndexes(): void {
    const indexes: Record<string, number> = {};
    for (const group of this.groupedArticles()) {
      indexes[group.label] = 0;
    }
    this.articleIndexes.set(indexes);
  }

  private loadUpcomingBirthdays(): void {
    // Skip the backend call during SSR; hydration re-runs this in the browser.
    if (!this.isBrowser) return;

    this.employeeDetailsService.getUpcomingBirthdays(30).subscribe({
      next: (list) => {
        this.upcomingBirthdays.set(list ?? []);
        this.birthdayPage.set(1);
        this.computeBirthdayMarkers();
      },
      error: (err) => console.warn('⚠️ Failed to load upcoming birthdays:', err),
    });
  }

  pagedBirthdays(): UpcomingBirthday[] {
    const start = (this.birthdayPage() - 1) * this.birthdaysPerPage;
    return this.upcomingBirthdays().slice(start, start + this.birthdaysPerPage);
  }

  birthdayPageCount(): number {
    return Math.ceil(this.upcomingBirthdays().length / this.birthdaysPerPage);
  }

  birthdayPages(): number[] {
    return Array.from({ length: this.birthdayPageCount() }, (_, i) => i + 1);
  }

  fullName(b: UpcomingBirthday): string {
    const name = `${b.firstName ?? ''} ${b.lastName ?? ''}`.trim();
    return name || 'Collaborateur';
  }

  initials(b: UpcomingBirthday): string {
    const first = b.firstName?.charAt(0) ?? '';
    const last = b.lastName?.charAt(0) ?? '';
    return (first + last).toUpperCase() || '?';
  }

  /** e.g. "15 juillet · Dans 3 jours" (or "· Aujourd'hui" / "· Demain"). */
  birthdaySubtitle(b: UpcomingBirthday): string {
    const month = SecondSectionComponent.MONTHS_FR[b.month - 1] ?? '';
    const date = `${b.day} ${month}`.trim();
    return `${date} · ${this.countdownLabel(b)}`;
  }

  private countdownLabel(b: UpcomingBirthday): string {
    if (b.today || b.daysUntil === 0) return "Aujourd'hui";
    if (b.daysUntil === 1) return 'Demain';
    return `Dans ${b.daysUntil} jours`;
  }

  /** Rebuild the day → names map for the month currently shown in the calendar. */
  private computeBirthdayMarkers(): void {
    const map = new Map<number, string[]>();
    for (const b of this.upcomingBirthdays()) {
      const day = this.birthdayDayInDisplayedMonth(b);
      if (day === null) continue;
      const names = map.get(day) ?? [];
      names.push(this.fullName(b));
      map.set(day, names);
    }
    this.birthdaysByDay = map;
  }

  /** Day of month the birthday lands on if it falls in the displayed month/year, else null. */
  private birthdayDayInDisplayedMonth(b: UpcomingBirthday): number | null {
    // Prefer the exact next occurrence (yyyy-MM-dd) so the year is respected.
    const parts = b.nextBirthday?.split('-').map(Number);
    if (parts?.length === 3 && parts.every((n) => !Number.isNaN(n))) {
      const [y, m, d] = parts;
      return y === this.year && m === this.month ? d : null;
    }
    return b.month === this.month ? b.day : null;
  }

  /** True when at least one collaborator has a birthday on this calendar day. */
  isBirthday(day: number | null): boolean {
    return day !== null && this.birthdaysByDay.has(day);
  }

  /** Tooltip listing the collaborators celebrating on this calendar day. */
  birthdayLabel(day: number | null): string {
    if (day === null) return '';
    const names = this.birthdaysByDay.get(day);
    return names ? `🎂 ${names.join(', ')}` : '';
  }

  // Méthodes du calendrier
  generateCalendar(): void {
    const firstWeekday = new Date(this.year, this.month - 1, 1).getDay();
    const daysInMonth = new Date(this.year, this.month, 0).getDate();

    this.calendarCells = [
      ...Array(firstWeekday).fill(null),
      ...Array.from({ length: daysInMonth }, (_, i) => i + 1),
    ];

    while (this.calendarCells.length % 7 !== 0) {
      this.calendarCells.push(null);
    }
  }

  prevMonth(): void {
    if (this.month === 1) {
      this.year--;
      this.month = 12;
    } else {
      this.month--;
    }
    this.updateCalendar();
  }

  nextMonth(): void {
    if (this.month === 12) {
      this.year++;
      this.month = 1;
    } else {
      this.month++;
    }
    this.updateCalendar();
  }

  private updateCalendar(): void {
    this.monthName = new Date(this.year, this.month - 1, 1).toLocaleString('fr-FR', {
      month: 'long',
    });
    this.generateCalendar();
    this.computeBirthdayMarkers();
    this.selectedDay =
      this.year === this.now.getFullYear() && this.month === this.now.getMonth() + 1
        ? this.now.getDate()
        : null;
  }

  selectDay(day: number | null): void {
    if (day) {
      this.selectedDay = day;
      console.log('Jour sélectionné:', day);
    }
  }

  isToday(day: number | null): boolean {
    return (
      day !== null &&
      day === this.now.getDate() &&
      this.year === this.now.getFullYear() &&
      this.month === this.now.getMonth() + 1
    );
  }

  isSunday(index: number): boolean {
    return index % 7 === 0;
  }

  trackByIndex(index: number): number {
    return index;
  }

  openArticle(article: Article): void {
    void this.router.navigate(['/actualites', article.id]);
  }

  ngOnDestroy(): void {
    for (const url of this.coverObjectUrls) URL.revokeObjectURL(url);
    this.coverObjectUrls = [];
  }
}

import { isPlatformBrowser } from '@angular/common';
import {
  Component,
  ElementRef,
  HostListener,
  OnDestroy,
  PLATFORM_ID,
  computed,
  effect,
  inject,
  input,
  signal,
  viewChild,
} from '@angular/core';

/** One gallery photo. The parent owns the object URL and revokes it. */
export interface GalleryPhoto {
  id: number;
  url: string;
  alt: string;
}

/** How many tiles the mosaic shows before collapsing the rest behind a "+N" overlay. */
const MAX_TILES = 5;

@Component({
  selector: 'app-article-gallery',
  standalone: true,
  imports: [],
  templateUrl: './article-gallery-component.html',
  styles: [
    `
      :host {
        display: block;
      }

      @keyframes gallery-fade-in {
        from {
          opacity: 0;
        }
        to {
          opacity: 1;
        }
      }

      .gallery-viewer {
        animation: gallery-fade-in 0.18s ease-out;
      }

      @media (prefers-reduced-motion: reduce) {
        .gallery-viewer {
          animation: none;
        }
      }
    `,
  ],
})
export class ArticleGalleryComponent implements OnDestroy {
  private readonly isBrowser = isPlatformBrowser(inject(PLATFORM_ID));

  readonly photos = input.required<GalleryPhoto[]>();

  /** Index of the photo shown full screen, or null when the viewer is closed. */
  readonly openIndex = signal<number | null>(null);

  /** The element that opened the viewer, so focus can go back to it on close. */
  private opener: HTMLElement | null = null;

  private readonly closeButton = viewChild<ElementRef<HTMLButtonElement>>('closeButton');

  constructor() {
    // Move focus into the viewer as soon as it renders; `close()` hands it back to the tile.
    effect(() => this.closeButton()?.nativeElement.focus());
  }

  readonly tiles = computed(() => this.photos().slice(0, MAX_TILES));

  /** Photos hidden behind the last tile's "+N" overlay. */
  readonly overflowCount = computed(() => Math.max(0, this.photos().length - MAX_TILES));

  /**
   * The layout bucket. Each cardinality gets its own composition rather than one uniform
   * grid — that is the point of the mosaic.
   */
  readonly layout = computed<'single' | 'pair' | 'trio' | 'mosaic'>(() => {
    const count = this.photos().length;
    if (count === 1) return 'single';
    if (count === 2) return 'pair';
    if (count === 3) return 'trio';
    return 'mosaic';
  });

  readonly currentPhoto = computed(() => {
    const index = this.openIndex();
    return index === null ? null : (this.photos()[index] ?? null);
  });

  open(index: number, event: Event): void {
    this.opener = (event.currentTarget as HTMLElement) ?? null;
    this.openIndex.set(index);
    this.lockScroll(true);
  }

  close(): void {
    this.openIndex.set(null);
    this.lockScroll(false);
    this.opener?.focus();
    this.opener = null;
  }

  /** Wraps around, so the arrows never dead-end. */
  step(direction: 1 | -1): void {
    const total = this.photos().length;
    if (total === 0) return;
    this.openIndex.update((index) => (index === null ? null : (index + direction + total) % total));
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.openIndex() !== null) this.close();
  }

  @HostListener('document:keydown.arrowright')
  onArrowRight(): void {
    if (this.openIndex() !== null) this.step(1);
  }

  @HostListener('document:keydown.arrowleft')
  onArrowLeft(): void {
    if (this.openIndex() !== null) this.step(-1);
  }

  ngOnDestroy(): void {
    // Navigating away with the viewer open would otherwise leave the page unscrollable.
    this.lockScroll(false);
  }

  private lockScroll(locked: boolean): void {
    if (!this.isBrowser) return;
    document.body.style.overflow = locked ? 'hidden' : '';
  }
}

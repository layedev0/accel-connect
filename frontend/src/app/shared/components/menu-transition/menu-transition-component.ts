import { Component, Output, EventEmitter, OnInit, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LottieComponent, AnimationOptions } from 'ngx-lottie';
import { AnimationItem } from 'lottie-web';

interface AnimationTheme {
  primary: string;
  secondary: string;
}

@Component({
  selector: 'app-menu-transition',
  standalone: true,
  imports: [CommonModule, LottieComponent],
  templateUrl: './menu-transition-component.html',
  styleUrl: './menu-transition-component.css',
})
export class MenuTransitionComponent implements OnInit {
  @Output() animationComplete = new EventEmitter<void>();

  // 🆕 Inputs pour personnaliser chaque animation
  @Input() animationPath: string = '/assets/lottie/Burger.json';
  @Input() animationTitle: string = 'Chargement du Menu';
  @Input() theme: AnimationTheme = { primary: '#25509D', secondary: '#99CFBD' };
  @Input() duration: number = 1000; // Durée en ms

  isVisible = false;
  lottieOptions: AnimationOptions = {
    path: this.animationPath,
    loop: true,
    autoplay: true,
  };

  private animationItem: AnimationItem | null = null;

  ngOnInit(): void {
    // Mettre à jour les options avec le path reçu
    this.lottieOptions = {
      path: this.animationPath,
      loop: true,
      autoplay: true,
    };

    // Rendre visible avec un léger délai pour l'animation d'entrée
    setTimeout(() => {
      this.isVisible = true;
    }, 50);

    // Durée totale de l'animation (personnalisable)
    setTimeout(() => {
      this.closeAnimation();
    }, this.duration);
  }

  onAnimationCreated(animationItem: AnimationItem): void {
    this.animationItem = animationItem;
  }

  private closeAnimation(): void {
    this.isVisible = false;
    setTimeout(() => {
      this.animationComplete.emit();
    }, 500);
  }

  // Méthodes utilitaires pour diviser le titre
  getFirstWord(): string {
    const words = this.animationTitle.split(' ');
    return words[0] || '';
  }

  getSecondWord(): string {
    const words = this.animationTitle.split(' ');
    return words.slice(1, -1).join(' ') || '';
  }

  getLastWord(): string {
    const words = this.animationTitle.split(' ');
    return words[words.length - 1] || '';
  }

  // Ajuster la luminosité d'une couleur hex
  adjustBrightness(hex: string, percent: number): string {
    const num = parseInt(hex.replace('#', ''), 16);
    const amt = Math.round(2.55 * percent);
    const R = (num >> 16) + amt;
    const G = ((num >> 8) & 0x00ff) + amt;
    const B = (num & 0x0000ff) + amt;

    return (
      '#' +
      (
        0x1000000 +
        (R < 255 ? (R < 1 ? 0 : R) : 255) * 0x10000 +
        (G < 255 ? (G < 1 ? 0 : G) : 255) * 0x100 +
        (B < 255 ? (B < 1 ? 0 : B) : 255)
      )
        .toString(16)
        .slice(1)
    );
  }
}

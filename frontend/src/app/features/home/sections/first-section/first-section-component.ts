import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { Router, RouterModule } from '@angular/router';
import { AppConfig } from '../../../../core/config/app.config';
import { MenuTransitionComponent } from '../../../../shared/components/menu-transition/menu-transition-component';
import { AlexChatComponent } from '../../alex-chat/alex-chat-component';
import { KeycloakService } from '../../../auth/service/keycloak.service';
import { UserService, User } from '../../../auth/service/user.service';

@Component({
  selector: 'app-first-section-component',
  standalone: true,
  imports: [CommonModule, FontAwesomeModule, RouterModule, MenuTransitionComponent, AlexChatComponent],
  templateUrl: './first-section-component.html',
  styleUrl: './first-section-component.css',
})
export class FirstSectionComponent implements OnInit {
  private readonly keycloakService = inject(KeycloakService);
  private readonly userService = inject(UserService);
  private readonly router = inject(Router);

  // Signaux pour les données utilisateur
  userName = signal<string>('');
  isLoading = signal(true);

  chatOpen = false;
  icons = AppConfig.icons;
  iconTitles = AppConfig.iconTitles;
  iconRoutes = AppConfig.iconRoutes;

  lottieAnimations = AppConfig.lottieAnimations;
  animationTitles = AppConfig.animationTitles;
  animationThemes = AppConfig.animationThemes;

  showTransition = false;
  pendingRoute: string | null = null;
  currentAnimationIndex: number = 0;

  ngOnInit(): void {
    this.loadUserName();
  }

  /**
   * Charge le nom de l'utilisateur depuis le backend
   * Fallback sur Keycloak si le backend échoue
   */
  private loadUserName(): void {
    if (!this.keycloakService.isAuthenticated()) {
      this.userName.set('');
      this.isLoading.set(false);
      return;
    }

    // Essayer de charger depuis le backend
    this.userService.getCurrentUser().subscribe({
      next: (user: User) => {
        const firstName = user.firstName || user.username || 'Utilisateur';
        this.userName.set(this.capitalizeFirstLetter(firstName));
        this.isLoading.set(false);
        // console.log('👤 User name loaded:', firstName);
      },
      error: (error) => {
        console.warn('⚠️ Failed to load user from backend, using Keycloak info:', error);
        // Fallback sur Keycloak
        const keycloakInfo = this.keycloakService.getUserInfo();
        const firstName = keycloakInfo.firstName || keycloakInfo.username || 'Utilisateur';
        this.userName.set(this.capitalizeFirstLetter(firstName));
        this.isLoading.set(false);
      },
    });
  }

  /**
   * Capitalise la première lettre d'un prénom
   */
  private capitalizeFirstLetter(name: string): string {
    if (!name) return 'Utilisateur';
    return name.charAt(0).toUpperCase() + name.slice(1).toLowerCase();
  }

  /**
   * Retourne le message de salutation complet
   */
  getGreetingMessage(): string {
    return `Dalal ak jàmm, ${this.userName()}!`;
  }

  /**
   * Retourne juste le nom pour l'affichage stylé
   */
  getDisplayName(): string {
    return this.userName();
  }

  onIconClick(event: Event, index: number): void {
    event.preventDefault();
    this.router.navigate([this.iconRoutes[index]]);
  }

  // 🆕 Méthode appelée quand l'animation est terminée
  onTransitionComplete(): void {
    this.showTransition = false;
    if (this.pendingRoute) {
      this.router.navigate([this.pendingRoute]);
      this.pendingRoute = null;
    }
  }

  // 🆕 Obtenir l'animation actuelle
  getCurrentAnimation(): string {
    return this.lottieAnimations[this.currentAnimationIndex] || this.lottieAnimations[0];
  }

  // 🆕 Obtenir le titre de l'animation actuelle
  getCurrentAnimationTitle(): string {
    return this.animationTitles[this.currentAnimationIndex] || this.animationTitles[0];
  }

  // 🆕 Obtenir le thème de l'animation actuelle
  getCurrentAnimationTheme(): { primary: string; secondary: string } {
    return this.animationThemes[this.currentAnimationIndex] || this.animationThemes[0];
  }

  toggleChat(): void {
    this.chatOpen = !this.chatOpen;
  }
}

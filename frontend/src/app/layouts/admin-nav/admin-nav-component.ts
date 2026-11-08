import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { NavigationLink } from './navlink';
import { AppConfig } from '../../core/config/app.config';
import { KeycloakService } from '../../features/auth/service/keycloak.service';
import { UserService, User } from '../../features/auth/service/user.service';

@Component({
  selector: 'app-admin-nav-component',
  standalone: true,
  imports: [CommonModule, RouterModule, FontAwesomeModule],
  templateUrl: './admin-nav-component.html',
  styleUrl: './admin-nav-component.css',
})
export class AdminNavComponent implements OnInit {
  private readonly keycloakService = inject(KeycloakService);
  private readonly userService = inject(UserService);

  mobileMenuOpen = false;
  userMenuOpen = false;
  logoUrl = AppConfig.logoUrl;

  // Signals pour la réactivité
  isAuthenticated = signal(false);
  currentUser = signal<User | null>(null);
  avatarUrl = signal('https://ui-avatars.com/api/?name=Admin&background=3b82f6&color=fff');

  navigationLinks: NavigationLink[] = [
    { name: 'Dashboard', route: '/admin', icon: 'house' },
    { name: 'Assigner Plat', route: '/admin/add-food-to-restau', icon: 'utensils' },
    { name: 'Plats', route: '/admin/liste-food', icon: 'list' },
    { name: 'Restaurants', route: '/admin/liste-restaurant', icon: 'list' },
    { name: 'Solde', route: '/admin/wallets', icon: 'wallet' },
    { name: 'Stagiaires', route: '/admin/interns', icon: 'user-graduate' },
    { name: 'Restauration', route: '/restauration', icon: 'utensils' },
    { name: 'Home', route: '/', icon: 'home' },
  ];

  ngOnInit() {
    this.checkAuthentication();
  }

  private checkAuthentication() {
    this.isAuthenticated.set(this.keycloakService.isAuthenticated());

    if (this.isAuthenticated()) {
      this.loadCurrentUser();
    }
  }

  private loadCurrentUser() {
    this.userService.getCurrentUser().subscribe({
      next: (user) => {
        // console.log('👤 Admin user loaded:', user);
        this.currentUser.set(user);
        this.updateAvatar(user);
      },
      error: (error) => {
        console.error('❌ Failed to load admin user:', error);
        // Fallback sur les infos Keycloak
        const keycloakInfo = this.keycloakService.getUserInfo();
        if (keycloakInfo.firstName || keycloakInfo.lastName) {
          const name = `${keycloakInfo.firstName || ''} ${keycloakInfo.lastName || ''}`.trim();
          this.avatarUrl.set(
            `https://ui-avatars.com/api/?name=${encodeURIComponent(
              name
            )}&background=3b82f6&color=fff`
          );
        }
      },
    });
  }

  private updateAvatar(user: User) {
    const name = `${user.firstName || ''} ${user.lastName || ''}`.trim() || user.username;
    this.avatarUrl.set(
      `https://ui-avatars.com/api/?name=${encodeURIComponent(
        name
      )}&background=3b82f6&color=fff&bold=true`
    );
  }

  toggleMobileMenu() {
    this.mobileMenuOpen = !this.mobileMenuOpen;
  }

  toggleUserMenu() {
    this.userMenuOpen = !this.userMenuOpen;
  }

  closeMobileMenu() {
    this.mobileMenuOpen = false;
  }

  logout() {
    this.keycloakService.logout();
    this.userService.clearCurrentUser();
    this.currentUser.set(null);
    this.isAuthenticated.set(false);
  }

  getDisplayName(): string {
    const user = this.currentUser();
    if (user) {
      return `${user.firstName || ''} ${user.lastName || ''}`.trim() || user.username;
    }

    const keycloakInfo = this.keycloakService.getUserInfo();
    return (
      `${keycloakInfo.firstName || ''} ${keycloakInfo.lastName || ''}`.trim() ||
      keycloakInfo.username ||
      'Administrateur'
    );
  }

  getDisplayEmail(): string {
    const user = this.currentUser();
    if (user?.email) return user.email;

    const keycloakInfo = this.keycloakService.getUserInfo();
    return keycloakInfo.email || 'admin@restaurant.com';
  }
}

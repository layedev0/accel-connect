import { Injectable, PLATFORM_ID, inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { Router } from '@angular/router';
import Keycloak from 'keycloak-js';
import { ExtendedKeycloakTokenParsed } from '../model/keycloak.model';
import { environment } from '../../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class KeycloakService {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly router = inject(Router);
  private keycloak!: Keycloak;
  private initialized = false;
  private initPromise: Promise<boolean> | null = null;

  async init(): Promise<boolean> {
    if (!isPlatformBrowser(this.platformId)) {
      return false;
    }

    if (this.initPromise) return this.initPromise;
    if (this.initialized) return true;

    this.initPromise = this.performInit();
    return this.initPromise;
  }

  private async performInit(): Promise<boolean> {
    try {
      this.keycloak = new Keycloak({
        url: `${environment.keycloak.url}`,
        realm: `${environment.keycloak.realm}`,
        clientId: `${environment.keycloak.clientId}`,
      });

      const authenticated = await this.keycloak.init({
        onLoad: 'check-sso',
        checkLoginIframe: false,
        enableLogging: false,
        pkceMethod: 'S256',
      });

      this.initialized = true;

      if (authenticated) {
        this.setupTokenRefresh();

        // ✅ Rediriger l'admin vers /admin après connexion
        this.redirectAdminIfNeeded();
      }

      return authenticated;
    } catch (error) {
      console.error('❌ Keycloak initialization failed:', error);
      this.initialized = false;
      this.initPromise = null;
      return false;
    }
  }

  /**
   * 🔀 Redirige l'admin vers /admin s'il est sur la page d'accueil
   */
  private redirectAdminIfNeeded(): void {
    const currentPath = window.location.pathname;

    // Si l'utilisateur est admin et sur la page d'accueil, rediriger vers /admin
    if (this.isAdmin() && currentPath === '/') {
      console.log('🔀 Admin détecté, redirection vers /admin');
      this.router.navigate(['/admin']);
    }
  }

  /**
   * 🔐 Login intelligent :
   * - Admin → redirection vers /admin
   * - User normal → redirection vers /
   */
  async login(): Promise<void> {
    if (!isPlatformBrowser(this.platformId)) return;

    // Déterminer la redirection avant le login
    const redirectUri = window.location.origin;

    try {
      await this.keycloak.login({ redirectUri });
    } catch (error) {
      console.error('❌ Login failed:', error);
    }
  }

  async logout(): Promise<void> {
    if (!isPlatformBrowser(this.platformId)) return;

    try {
      await this.keycloak.logout({
        redirectUri: window.location.origin,
      });
    } catch (error) {
      console.error('❌ Logout failed:', error);
    }
  }

  private setupTokenRefresh(): void {
    if (!isPlatformBrowser(this.platformId)) return;

    setInterval(() => {
      this.keycloak.updateToken(70).catch(() => {
        console.error('❌ Failed to refresh token');
        this.login();
      });
    }, 60000);
  }

  getToken(): string | undefined {
    if (!isPlatformBrowser(this.platformId) || !this.initialized) return undefined;
    return this.keycloak?.token;
  }

  isAuthenticated(): boolean {
    if (!isPlatformBrowser(this.platformId) || !this.initialized) return false;
    return !!this.keycloak?.authenticated;
  }

  getTokenParsed(): ExtendedKeycloakTokenParsed | undefined {
    if (!isPlatformBrowser(this.platformId) || !this.initialized) return undefined;
    return this.keycloak?.tokenParsed as ExtendedKeycloakTokenParsed;
  }

  getUserInfo() {
    const token = this.getTokenParsed();
    return {
      username: token?.['preferred_username'],
      email: token?.['email'],
      firstName: token?.['given_name'],
      lastName: token?.['family_name'],
      sub: token?.['sub'],
    };
  }

  /** Vérifie si l'utilisateur a un rôle spécifique */
  hasRole(role: string): boolean {
    return this.hasAnyRole([role]);
  }

  /** Vérifie si l'utilisateur a au moins un des rôles */
  hasAnyRole(requiredRoles: string[]): boolean {
    if (!isPlatformBrowser(this.platformId) || !this.initialized) return false;

    const token = this.getTokenParsed();
    if (!token) return false;

    const realmRoles = token?.realm_access?.roles || [];
    const clientRoles = token?.resource_access?.[environment.keycloak.clientId]?.roles || [];

    const allRoles = new Set([...realmRoles, ...clientRoles]);

    return requiredRoles.some((role) =>
      Array.from(allRoles).some((r) => r.toLowerCase() === role.toLowerCase())
    );
  }

  /** 👑 Admin uniquement */
  isAdmin(): boolean {
    return this.hasAnyRole(['ROLE_ADMIN', 'admin']);
  }

  /** 📋 Tous les rôles */
  getUserRoles(): string[] {
    if (!isPlatformBrowser(this.platformId) || !this.initialized) return [];

    const token = this.getTokenParsed();
    if (!token) return [];

    const realmRoles = token.realm_access?.roles || [];
    const clientRoles = token.resource_access?.[environment.keycloak.clientId]?.roles || [];

    return [...new Set([...realmRoles, ...clientRoles])];
  }
}

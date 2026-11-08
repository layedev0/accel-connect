/**
 * 🔐 AuthGuard
 * Vérifie si l'utilisateur est authentifié via Keycloak.
 * ✅ Attend que Keycloak soit complètement initialisé avant d'autoriser l'accès
 */
import { inject, PLATFORM_ID } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { KeycloakService } from '../service/keycloak.service';
import { isPlatformBrowser } from '@angular/common';

export const authGuard: CanActivateFn = async () => {
  const platformId = inject(PLATFORM_ID);

  // ⚠️ En SSR : on ne bloque jamais
  if (!isPlatformBrowser(platformId)) return true;

  const keycloak = inject(KeycloakService);
  const router = inject(Router);

  try {
    // ✅ CRITIQUE : Attendre que Keycloak soit initialisé
    const authenticated = await keycloak.init();

    if (authenticated && keycloak.getToken()) {
      // console.log('✅ User authenticated with token');
      return true;
    }

    // Pas authentifié → login Keycloak
    console.log('⚠️ Not authenticated, redirecting to login');
    await keycloak.login();
    return false;
  } catch (err) {
    console.error('❌ Auth guard error:', err);
    router.navigate(['/']);
    return false;
  }
};

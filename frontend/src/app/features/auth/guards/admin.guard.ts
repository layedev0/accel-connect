import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { KeycloakService } from '../service/keycloak.service';
/**
 * 🔐 Guard pour les routes admin
 * Redirige automatiquement les admins vers /admin
 */
export const adminGuard: CanActivateFn = (route, state) => {
  const keycloakService = inject(KeycloakService);
  const router = inject(Router);

  const isAdmin = keycloakService.hasRole('ROLE_ADMIN');

  if (isAdmin) {
    // Si l'admin essaie d'accéder à une route non-admin, rediriger vers /admin
    if (!state.url.startsWith('/admin')) {
      router.navigate(['/admin']);
      return false;
    }
    return true;
  }

  // Si non-admin essaie d'accéder à /admin, rediriger vers /restauration
  if (state.url.startsWith('/admin')) {
    router.navigate(['/']);
    return false;
  }

  return true;
};

/**
 * 🏠 Guard pour redirection automatique selon le rôle
 */
export const roleRedirectGuard: CanActivateFn = () => {
  const keycloakService = inject(KeycloakService);
  const router = inject(Router);

  if (keycloakService.isAuthenticated()) {
    const isAdmin = keycloakService.hasRole('ROLE_ADMIN');

    if (isAdmin) {
      router.navigate(['/admin']);
    } else {
      router.navigate(['/']);
    }

    return false;
  }

  return true;
};

/**
 * 🛡️ AdminOnlyGuard
 * Autorise uniquement les utilisateurs avec le rôle ADMIN
 * sur les routes commençant par /admin.
 *
 * - Si utilisateur normal → redirection vers /
 * - Si admin → accès autorisé
 */
import { inject, PLATFORM_ID } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { KeycloakService } from '../service/keycloak.service';
import { isPlatformBrowser } from '@angular/common';

export const adminOnlyGuard: CanActivateFn = async (route, state) => {
  const platformId = inject(PLATFORM_ID);

  // SSR : autoriser (sera vérifié côté client)
  if (!isPlatformBrowser(platformId)) return true;

  const keycloak = inject(KeycloakService);
  const router = inject(Router);

  // Attendre l'initialisation de Keycloak
  await keycloak.init();

  if (!keycloak.isAuthenticated()) {
    console.log('⚠️ Non authentifié, redirection vers login');
    await keycloak.login();
    return false;
  }

  const isAdmin = keycloak.isAdmin();

  if (isAdmin) {
    console.log('✅ Admin autorisé sur:', state.url);
    return true;
  }

  // 🚫 Non admin → redirection vers l'accueil
  console.warn('❌ Accès admin refusé pour:', keycloak.getUserInfo().username);
  router.navigate(['/']);
  return false;
};

/**
 * 🛡️ AdminRHGuard
 * Autorise uniquement les utilisateurs avec le rôle HR_MANAGER
 * pour les routes Admin RH (ex: /rh-admin).
 */
export const adminRHGuard: CanActivateFn = async (route, state) => {
  const platformId = inject(PLATFORM_ID);

  if (!isPlatformBrowser(platformId)) return true;

  const keycloak = inject(KeycloakService);
  const router = inject(Router);

  await keycloak.init();

  if (!keycloak.isAuthenticated()) {
    await keycloak.login();
    return false;
  }

  const isAdminRH = keycloak.hasRole('HR_MANAGER');

  if (isAdminRH) return true;

  router.navigate(['/']);
  return false;
};

/**
 * 🛡️ SuperAdminGuard
 * Autorise uniquement les utilisateurs avec le rôle SUPER_ADMIN
 * pour l'espace Super Admin (ex: /super-admin).
 * N'accorde AUCUN accès à l'espace RH (rôles distincts).
 */
export const superAdminGuard: CanActivateFn = async (route, state) => {
  const platformId = inject(PLATFORM_ID);

  if (!isPlatformBrowser(platformId)) return true;

  const keycloak = inject(KeycloakService);
  const router = inject(Router);

  await keycloak.init();

  if (!keycloak.isAuthenticated()) {
    await keycloak.login();
    return false;
  }

  if (keycloak.hasRole('SUPER_ADMIN')) return true;

  router.navigate(['/']);
  return false;
};

/**
 * 🛡️ ManagerGuard
 * Autorise les utilisateurs avec le rôle MANAGER ou HR_MANAGER
 * (ex: page d'approbation des congés).
 */
export const managerGuard: CanActivateFn = async (route, state) => {
  const platformId = inject(PLATFORM_ID);

  if (!isPlatformBrowser(platformId)) return true;

  const keycloak = inject(KeycloakService);
  const router = inject(Router);

  await keycloak.init();

  if (!keycloak.isAuthenticated()) {
    await keycloak.login();
    return false;
  }

  if (keycloak.hasAnyRole(['MANAGER', 'HR_MANAGER'])) return true;

  router.navigate(['/rh']);
  return false;
};

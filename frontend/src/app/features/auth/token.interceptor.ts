import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject, PLATFORM_ID } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { isPlatformBrowser } from '@angular/common';
import { KeycloakService } from './service/keycloak.service';
import { environment } from '../../../environments/environment';

export const tokenInterceptor: HttpInterceptorFn = (req, next) => {
  const platformId = inject(PLATFORM_ID);

  // ✅ En SSR, ne pas toucher aux requêtes
  if (!isPlatformBrowser(platformId)) {
    return next(req);
  }

  // ✅ CRITIQUE : Ajouter le token uniquement pour les appels vers VOTRE API
  if (!req.url.includes(environment.apiUrl)) {
    return next(req);
  }

  const keycloakService = inject(KeycloakService);
  const token = keycloakService.getToken();

  // Si pas de token, essayer de se connecter
  if (!token) {
    console.warn('⚠️ No token available, redirecting to login');
    keycloakService.login();
    return throwError(() => new HttpErrorResponse({ status: 401 }));
  }

  console.log('🔑 Adding Bearer token to request:', req.url);

  // Ajouter le token Bearer
  const clonedReq = req.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`,
    },
  });

  return next(clonedReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        console.error('❌ 401 Unauthorized - Token invalide, reconnexion');
        keycloakService.login();
      }
      return throwError(() => error);
    })
  );
};

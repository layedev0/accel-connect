import {
  ApplicationConfig,
  LOCALE_ID,
  APP_INITIALIZER,
  provideBrowserGlobalErrorListeners,
  provideZonelessChangeDetection,
  PLATFORM_ID,
} from '@angular/core';
import { provideRouter, withInMemoryScrolling } from '@angular/router';
import localeFr from '@angular/common/locales/fr';
import { provideLottieOptions } from 'ngx-lottie';
import player from 'lottie-web';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { isPlatformBrowser, registerLocaleData } from '@angular/common';

import { routes } from './app.routes';
import { provideClientHydration, withEventReplay } from '@angular/platform-browser';
import { KeycloakService } from './features/auth/service/keycloak.service';
import { tokenInterceptor } from './features/auth/token.interceptor';

/**
 * ✅ CRITIQUE : Initialiser Keycloak AVANT toute autre chose
 * Cette fonction bloque le démarrage de l'app jusqu'à ce que Keycloak soit prêt
 */
export function initializeKeycloak(
  keycloakService: KeycloakService,
  platformId: Object
): () => Promise<boolean> {
  return () => {
    if (isPlatformBrowser(platformId)) {
      console.log('🔐 Initializing Keycloak... (blocking app startup)');

      return keycloakService
        .init()
        .then((authenticated) => {
          if (authenticated) {
            const token = keycloakService.getToken();
            console.log('✅ Keycloak initialized successfully');
            console.log('🔑 Token available:', !!token);
            console.log('👤 User:', keycloakService.getUserInfo().username);
            console.log('🎭 Roles:', keycloakService.getUserRoles());
          } else {
            console.log('ℹ️ Keycloak initialized, but user not authenticated');
          }
          return authenticated;
        })
        .catch((error) => {
          console.error('❌ Keycloak initialization failed:', error);
          return false;
        });
    }

    console.log('⚠️ Skipping Keycloak initialization (SSR)');
    return Promise.resolve(false);
  };
}

registerLocaleData(localeFr);

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZonelessChangeDetection(),
    { provide: LOCALE_ID, useValue: 'fr-FR' },
    provideRouter(routes, withInMemoryScrolling({ scrollPositionRestoration: 'top' })),
    provideClientHydration(withEventReplay()),
    provideHttpClient(withFetch(), withInterceptors([tokenInterceptor])),
    provideLottieOptions({
      player: () => player,
    }),
    {
      provide: APP_INITIALIZER,
      useFactory: initializeKeycloak,
      multi: true,
      deps: [KeycloakService, PLATFORM_ID],
    },
  ],
};

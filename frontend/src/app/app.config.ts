import { ApplicationConfig, provideBrowserGlobalErrorListeners, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { providePrimeNG } from 'primeng/config';
import { authInterceptor } from './core/auth/auth.interceptor';
import { AristocrataPreset } from './core/theme/aristocrata-preset';
import { PRIMENG_PT_BR } from './core/theme/primeng-pt-br';
import { ConfirmationService, MessageService } from 'primeng/api';
import { erroHttpInterceptor,  } from './core/http/erro-http.interceptor';
import { environment } from '../environments/environment';
import { from, switchMap } from 'rxjs';
import type { HttpInterceptorFn } from '@angular/common/http';

// Carrega o mock sob demanda: só é buscado quando environment.usarMock é true (ng serve -c mock).
const mockInterceptorLazy: HttpInterceptorFn = (req, next) =>
  from(import('./core/http/mock.interceptor')).pipe(switchMap((m) => m.mockInterceptor(req, next)));

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    MessageService,
    ConfirmationService,
    provideHttpClient(withInterceptors([
      ...(environment.usarMock ? [mockInterceptorLazy] : []),
      authInterceptor,
      erroHttpInterceptor,
    ])),
    provideAnimationsAsync(),
    providePrimeNG({
      translation: PRIMENG_PT_BR,
      theme: {
        preset: AristocrataPreset,
        options: { darkModeSelector: '.app-dark' },
      },
    }),
  ],
};

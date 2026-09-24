import { ApplicationConfig, provideBrowserGlobalErrorListeners, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { providePrimeNG } from 'primeng/config';
import { authInterceptor } from './core/auth/auth.interceptor';
import { AristocrataPreset } from './core/theme/aristocrata-preset';
import { ConfirmationService, MessageService } from 'primeng/api';
import { erroHttpInterceptor } from './core/http/erro-http.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    MessageService,
    ConfirmationService,
    provideHttpClient(withInterceptors([authInterceptor, erroHttpInterceptor])),
    provideAnimationsAsync(),
    providePrimeNG({
      theme: {
        preset: AristocrataPreset,
        options: { darkModeSelector: '.app-dark' },
      },
    }),
  ],
};

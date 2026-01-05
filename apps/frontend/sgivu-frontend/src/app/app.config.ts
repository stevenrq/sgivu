import {
  ApplicationConfig,
  inject,
  LOCALE_ID,
  provideAppInitializer,
  provideZoneChangeDetection,
} from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideCharts, withDefaultRegisterables } from 'ng2-charts';
import { defaultOAuthInterceptor } from './features/auth/interceptors/default-oauth.interceptor';
import { AuthService } from './features/auth/services/auth.service';
import { ThemeService } from './shared/services/theme.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withInterceptors([defaultOAuthInterceptor])),
    provideCharts(withDefaultRegisterables()),
    {
      provide: LOCALE_ID,
      useValue: 'es-CO',
    },
    provideAppInitializer(() => inject(ThemeService).initialize()),
    provideAppInitializer(() => inject(AuthService).initializeAuthentication()),
  ],
};

import { LOCALE_ID, ApplicationConfig } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localeEs from '@angular/common/locales/es';
import { provideRouter, withInMemoryScrolling } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';

import { routes } from './app.routes';
import { csrfInterceptor } from './core/http/csrf.interceptor';
import { apiErrorInterceptor } from './core/http/api-error.interceptor';

registerLocaleData(localeEs);

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes, withInMemoryScrolling({ anchorScrolling: 'enabled', scrollPositionRestoration: 'enabled' })),
    provideHttpClient(withInterceptors([csrfInterceptor, apiErrorInterceptor])),
    { provide: LOCALE_ID, useValue: 'es' }
  ]
};

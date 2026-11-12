import { HttpClient, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { switchMap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';

const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS']);

export const csrfInterceptor: HttpInterceptorFn = (request, next) => {
  const withCredentials = request.clone({ withCredentials: true });
  if (SAFE_METHODS.has(request.method)) return next(withCredentials);
  const http = inject(HttpClient);
  return http.get<void>(`${environment.apiBaseUrl}/csrf`, { withCredentials: true })
    .pipe(switchMap(() => {
      const token = readCookie('XSRF-TOKEN');
      if (!token) return throwError(() => new Error('No se pudo obtener el token CSRF'));
      return next(withCredentials.clone({ setHeaders: { 'X-XSRF-TOKEN': token } }));
    }));
};

function readCookie(name: string): string | null {
  const prefix = `${name}=`;
  const cookie = document.cookie.split(';').map(item => item.trim()).find(item => item.startsWith(prefix));
  return cookie ? decodeURIComponent(cookie.slice(prefix.length)) : null;
}

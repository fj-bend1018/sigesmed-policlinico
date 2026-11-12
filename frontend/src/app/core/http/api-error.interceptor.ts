import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { ErrorMessageService } from './error-message.service';

export const apiErrorInterceptor: HttpInterceptorFn = (request, next) => {
  const errors = inject(ErrorMessageService);
  return next(request).pipe(catchError((error: unknown) => {
    const message = error instanceof HttpErrorResponse
      ? (error.error?.message || (error.status === 0 ? 'No se pudo conectar con SIGESMED.' : 'La solicitud no pudo completarse.'))
      : 'Ocurrió un error inesperado.';
    errors.publish(message);
    return throwError(() => error);
  }));
};

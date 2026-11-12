import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { ErrorMessageService } from './error-message.service';
import { AuthService } from '../auth/auth.service';

export const apiErrorInterceptor: HttpInterceptorFn = (request, next) => {
  const errors = inject(ErrorMessageService);
  const auth = inject(AuthService);
  const router = inject(Router);

  return next(request).pipe(
    catchError((error: unknown) => {
      let message = 'Ocurrió un error inesperado. Por favor, intenta nuevamente.';

      if (error instanceof HttpErrorResponse) {
        if (error.status === 0) {
          message = 'No se pudo conectar con el servidor de SIGESMED. Por favor, verifica tu conexión a internet o que el sistema esté disponible.';
        } else if (error.status === 401) {
          const serverMsg = typeof error.error?.message === 'string' ? error.error.message : '';
          if (serverMsg.includes('bloqueada')) {
            message = 'Tu cuenta ha sido bloqueada temporalmente por 15 minutos debido a múltiples intentos fallidos.';
          } else if (serverMsg.includes('Credenciales')) {
            message = 'DNI, correo o contraseña incorrectos. Por favor, verifica tus datos.';
          } else {
            message = 'Tu sesión ha expirado o no estás autenticado. Por favor, inicia sesión para continuar.';
            if (!router.url.includes('/login') && !router.url.includes('/registro')) {
              auth.clearSession();
              router.navigate(['/login']);
            }
          }
        } else if (error.status === 403) {
          message = 'Acceso denegado: no tienes permisos para acceder a esta sección o ejecutar esta acción.';
        } else if (error.status === 409) {
          message = error.error?.message || 'Existe un conflicto con la solicitud (por ejemplo, el horario ya fue ocupado o tiene citas activas asociadas).';
        } else if (error.status === 404) {
          message = error.error?.message || 'El recurso solicitado no fue encontrado.';
        } else if (error.status === 400) {
          message = error.error?.message || 'Los datos enviados no son válidos. Revisa los campos del formulario.';
        } else if (error.status >= 500) {
          message = 'El servidor experimentó un inconveniente temporal. Por favor, intenta nuevamente en unos minutos.';
        } else if (error.error?.message) {
          message = error.error.message;
        }
      }

      errors.publish(message);
      return throwError(() => error);
    })
  );
};

import { inject } from '@angular/core';
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { environment } from '../../../../environments/environment';

/**
 * Interceptor HTTP para el patrón BFF.
 * Añade `withCredentials` para que la cookie de sesión del gateway viaje con cada request.
 * Captura errores 401 y redirige al flujo de login, excepto en:
 * - `/auth/session`: evita bucle infinito (el check inicial espera 401 si no hay sesión)
 * - `/logout`: el logout puede devolver 401 si la sesión ya expiró
 *
 * @returns Observable que maneja la petición HTTP con credenciales y captura 401 para redirigir al login.
 */
export const defaultOAuthInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const apiUrl = environment.apiUrl;

  if (!req.url.startsWith(apiUrl)) {
    return next(req);
  }

  const request = req.clone({ withCredentials: true });
  const isSessionCheck = req.url.includes('/auth/session');
  const isLogout = req.url.endsWith('/logout');

  return next(request).pipe(
    catchError((err) => {
      if (
        err instanceof HttpErrorResponse &&
        err.status === 401 &&
        !isSessionCheck &&
        !isLogout
      ) {
        console.error('Session expired or invalid. Redirecting to login.');
        authService.startLoginFlow(
          `${window.location.pathname}${window.location.search}`,
        );
      }
      return throwError(() => err);
    }),
  );
};

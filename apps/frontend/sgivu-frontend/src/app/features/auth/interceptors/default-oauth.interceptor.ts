import { inject } from '@angular/core';
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { environment } from '../../../../environments/environment';

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
        console.error(
          'La sesión expiró o no es válida. Redirigiendo para iniciar sesión.',
        );
        authService.startLoginFlow(
          `${window.location.pathname}${window.location.search}`,
        );
      }
      return throwError(() => err);
    }),
  );
};

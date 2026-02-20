import { Injectable, Signal, computed, inject, signal } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, catchError, firstValueFrom, of, tap } from 'rxjs';
import { User } from '../../users/models/user.model';
import { UserService } from '../../users/services/user.service';
import { environment } from '../../../../environments/environment';

interface AuthSessionResponse {
  authenticated: boolean;
  userId: string;
  username: string | null;
  rolesAndPermissions: string[];
  isAdmin: boolean;
}

/**
 * Servicio de autenticación que maneja el estado de sesión del usuario.
 * Proporciona métodos para iniciar el flujo de login, cerrar sesión,
 * y obtener información del usuario autenticado.
 */
@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly userService = inject(UserService);

  private readonly apiUrl = environment.apiUrl;

  // --- Signals como fuente de verdad ---

  private readonly _isAuthenticated = signal(false);
  private readonly _isDoneLoading = signal(false);
  private readonly _user = signal<User | null>(null);
  private readonly _session = signal<AuthSessionResponse | null>(null);

  // --- Signals de solo lectura ---

  public readonly isAuthenticated: Signal<boolean> =
    this._isAuthenticated.asReadonly();

  public readonly isDoneLoading: Signal<boolean> =
    this._isDoneLoading.asReadonly();

  public readonly currentAuthenticatedUser: Signal<User | null> =
    this._user.asReadonly();

  // --- Computed signals ---

  public readonly isReadyAndAuthenticated = computed(
    () => this._isAuthenticated() && this._isDoneLoading(),
  );

  public readonly userId = computed<number | null>(() => {
    const id = this._session()?.userId;
    if (!id) return null;
    const parsed = Number(id);
    return Number.isNaN(parsed) ? null : parsed;
  });

  public readonly username = computed<string | null>(
    () => this._session()?.username ?? null,
  );

  public readonly admin = computed<boolean>(
    () => this._session()?.isAdmin ?? false,
  );

  public readonly rolesAndPermissions = computed<Set<string>>(
    () => new Set(this._session()?.rolesAndPermissions ?? []),
  );

  // --- Observable wrappers para guards, interceptors y pipes async ---

  public readonly isAuthenticated$ = toObservable(this._isAuthenticated);
  public readonly isDoneLoading$ = toObservable(this._isDoneLoading);
  public readonly currentAuthenticatedUser$ = toObservable(this._user);
  public readonly isReadyAndAuthenticated$ = toObservable(
    this.isReadyAndAuthenticated,
  );

  public getCurrentAuthenticatedUser(): User | null {
    return this._user();
  }

  public getUserId(): number | null {
    return this.userId();
  }

  public getUsername(): string | null {
    return this.username();
  }

  public isAdmin(): boolean {
    return this.admin();
  }

  public getRolesAndPermissions(): Set<string> {
    return this.rolesAndPermissions();
  }

  /**
   * Consulta el estado de sesión en el gateway (BFF) para decidir si el usuario está autenticado.
   * Si existe sesión, carga los datos del usuario y aplica la navegación post-login.
   */
  public async initializeAuthentication(): Promise<void> {
    try {
      const session = await firstValueFrom(
        this.http.get<AuthSessionResponse>(`${this.apiUrl}/auth/session`).pipe(
          catchError((error: HttpErrorResponse) => {
            if (error.status !== 401) {
              console.error('Error validating session on the gateway', error);
            }
            return of(null);
          }),
        ),
      );

      if (session?.authenticated) {
        this._session.set(session);
        this._isAuthenticated.set(true);
        await this.fetchAndStoreCurrentAuthenticatedUser();
        if (this.isLoginCallback()) {
          this.navigateAfterLogin();
        }
      } else {
        this._session.set(null);
        this._isAuthenticated.set(false);
      }
    } finally {
      this._isDoneLoading.set(true);
    }
  }

  /**
   * Inicia el flujo OAuth 2.0 delegando el login al gateway.
   *
   * @param redirectUrl - Ruta a la que se redirige después de autenticarse.
   */
  public startLoginFlow(redirectUrl = '/dashboard'): void {
    sessionStorage.setItem('postLoginRedirectUrl', redirectUrl);
    this.redirectTo(`${this.apiUrl}/oauth2/authorization/sgivu-gateway`);
  }

  /**
   * Limpia el estado local de autenticación y delega el cierre de sesión al gateway.
   */
  public logout(): void {
    this._isAuthenticated.set(false);
    this._session.set(null);
    this._user.set(null);
    sessionStorage.removeItem('postLoginRedirectUrl');
    this.redirectTo(`${this.apiUrl}/logout`);
  }

  /**
   * Devuelve un observable que emite el estado de autenticación.
   * Si el usuario no está autenticado, inicia el flujo de inicio de sesión.
   * Este método está diseñado para ser utilizado en guards.
   *
   * @param redirectUrl La URL a la que redirigir después de un inicio de sesión exitoso.
   * @returns Un `Observable<boolean>` que es `true` si está autenticado, `false` en caso contrario.
   */
  public enforceAuthentication(redirectUrl: string): Observable<boolean> {
    return this.isAuthenticated$.pipe(
      tap((isAuthenticated) => {
        if (!isAuthenticated) {
          this.startLoginFlow(redirectUrl);
        }
      }),
    );
  }

  // --- Métodos privados ---

  private redirectTo(url: string): void {
    window.location.assign(url);
  }

  /**
   * Determina si la ruta actual es el callback. Lo que significa que el usuario acaba de autenticarse.
   * @returns `true` si la ruta actual es el callback de login, `false` en caso contrario.
   */
  private isLoginCallback(): boolean {
    return window.location.pathname.includes('/callback');
  }

  private navigateAfterLogin(): void {
    const redirectUrl =
      sessionStorage.getItem('postLoginRedirectUrl') ?? '/dashboard';
    sessionStorage.removeItem('postLoginRedirectUrl');
    this.router.navigateByUrl(redirectUrl, { replaceUrl: true });
  }

  /**
   * Obtiene el usuario autenticado desde el backend y lo almacena en el signal
   * para exponerlo reactivamente.
   *
   * @returns Una promesa que se resuelve cuando la operación finaliza.
   */
  private fetchAndStoreCurrentAuthenticatedUser(): Promise<void> {
    return new Promise((resolve) => {
      const id = this.userId();
      if (!id) {
        console.warn('User ID not found in session');
        this._user.set(null);
        resolve();
        return;
      }

      this.userService.getById(id).subscribe({
        next: (user) => {
          this._user.set(user);
          resolve();
        },
        error: (err) => {
          console.error('Failed to fetch current user', err);
          this._user.set(null);
          resolve();
        },
      });
    });
  }
}

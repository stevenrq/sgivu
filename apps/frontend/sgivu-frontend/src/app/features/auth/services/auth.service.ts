import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import {
  BehaviorSubject,
  combineLatest,
  firstValueFrom,
  map,
  Observable,
  of,
  tap,
  catchError,
} from 'rxjs';
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
 * Encapsula la integración BFF: valida la sesión en el gateway, gestiona el estado local
 * de autenticación y obtiene el usuario actual sin exponer tokens al navegador.
 */
@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly userService = inject(UserService);

  private readonly apiUrl = environment.apiUrl;

  private readonly isAuthenticatedSubject$ = new BehaviorSubject<boolean>(
    false,
  );
  private readonly isDoneLoadingSubject$ = new BehaviorSubject<boolean>(false);
  private readonly userSubject$ = new BehaviorSubject<User | null>(null);
  private readonly sessionSubject$ =
    new BehaviorSubject<AuthSessionResponse | null>(null);

  public readonly isAuthenticated$: Observable<boolean> =
    this.isAuthenticatedSubject$.asObservable();

  public readonly isDoneLoading$: Observable<boolean> =
    this.isDoneLoadingSubject$.asObservable();

  public readonly currentAuthenticatedUser$: Observable<User | null> =
    this.userSubject$.asObservable();

  /**
   * Un observable que emite `true` cuando la aplicación está lista para interactuar con rutas protegidas.
   *
   * Combina dos estados clave:
   * 1. La autenticación del usuario (`isAuthenticated$`).
   * 2. La finalización de los procesos de carga inicial de la aplicación (`isDoneLoading$`).
   *
   * Este observable es útil no solo para guardas de ruta, sino también para cualquier componente
   * que necesite determinar si debe mostrar el contenido principal de la aplicación o una pantalla de carga.
   *
   * @returns Un `Observable<boolean>` que es `true` si la aplicación está lista y el usuario está autenticado,
   * de lo contrario, `false`.
   */
  public readonly isReadyAndAuthenticated$: Observable<boolean> = combineLatest(
    [this.isAuthenticated$, this.isDoneLoading$],
  ).pipe(
    map(([isAuthenticated, isDoneLoading]) => isAuthenticated && isDoneLoading),
  );

  /**
   * Consulta el estado de sesión en el gateway para decidir si el usuario está autenticado.
   * Si existe sesión, carga los datos del usuario y aplica la navegación post-login.
   */
  public async initializeAuthentication(): Promise<void> {
    try {
      const session = await firstValueFrom(
        this.http.get<AuthSessionResponse>(`${this.apiUrl}/auth/session`).pipe(
          catchError((error: HttpErrorResponse) => {
            if (error.status !== 401) {
              console.error('Error al validar la sesión en el gateway', error);
            }
            return of(null);
          }),
        ),
      );

      if (session?.authenticated) {
        this.sessionSubject$.next(session);
        this.isAuthenticatedSubject$.next(true);
        await this.fetchAndStoreCurrentAuthenticatedUser();
        if (this.isLoginCallback()) {
          this.navigateAfterLogin();
        }
      } else {
        this.sessionSubject$.next(null);
        this.isAuthenticatedSubject$.next(false);
      }
    } finally {
      this.isDoneLoadingSubject$.next(true);
    }
  }

  /**
   * Inicia el flujo OAuth 2.0 delegando el login al gateway BFF.
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
    this.isAuthenticatedSubject$.next(false);
    this.sessionSubject$.next(null);
    this.userSubject$.next(null);
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

  public getCurrentAuthenticatedUser(): User | null {
    return this.userSubject$.value;
  }

  public getUserId(): number | null {
    const userId = this.sessionSubject$.value?.userId;
    if (!userId) {
      return null;
    }
    const parsed = Number(userId);
    return Number.isNaN(parsed) ? null : parsed;
  }

  public getUsername(): string | null {
    return this.sessionSubject$.value?.username ?? null;
  }

  public isAdmin(): boolean {
    return this.sessionSubject$.value?.isAdmin ?? false;
  }

  public getRolesAndPermissions(): Set<string> {
    return new Set(this.sessionSubject$.value?.rolesAndPermissions ?? []);
  }

  private redirectTo(url: string): void {
    window.location.assign(url);
  }

  /**
   * Recupera la ruta almacenada antes de iniciar sesión y navega hacia ella
   * al completar el callback del gateway.
   */
  private navigateAfterLogin(): void {
    const redirectUrl =
      sessionStorage.getItem('postLoginRedirectUrl') ?? '/dashboard';
    sessionStorage.removeItem('postLoginRedirectUrl');
    this.router.navigateByUrl(redirectUrl, { replaceUrl: true });
  }

  private isLoginCallback(): boolean {
    return window.location.pathname.includes('/callback');
  }

  /**
   * Obtiene el usuario autenticado desde el backend y lo almacena en el Subject
   * para exponerlo como observable. Regresa una promesa para facilitar awaits.
   *
   * @returns Promesa resuelta cuando finaliza la llamada al backend.
   */
  private fetchAndStoreCurrentAuthenticatedUser(): Promise<void> {
    return new Promise((resolve) => {
      const userId = this.getUserId();
      if (!userId) {
        console.warn('No se encontró el ID del usuario en la sesión');
        this.userSubject$.next(null);
        resolve();
        return;
      }

      this.userService.getById(userId).subscribe({
        next: (user) => {
          this.userSubject$.next(user);
          resolve();
        },
        error: (err) => {
          console.error('Falló la obtención del usuario actual', err);
          this.userSubject$.next(null);
          resolve();
        },
      });
    });
  }
}

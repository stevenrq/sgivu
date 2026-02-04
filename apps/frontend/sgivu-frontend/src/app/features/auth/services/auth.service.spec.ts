import { TestBed } from '@angular/core/testing';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';
import { AuthService } from '../services/auth.service';
import { UserService } from '../../users/services/user.service';
import { Router } from '@angular/router';
import { of, throwError, firstValueFrom } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { User } from '../../users/models/user.model';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let userServiceSpy: jasmine.SpyObj<UserService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(() => {
    userServiceSpy = jasmine.createSpyObj('UserService', ['getById']);
    routerSpy = jasmine.createSpyObj('Router', ['navigateByUrl']);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClientTesting(),
        { provide: UserService, useValue: userServiceSpy },
        { provide: Router, useValue: routerSpy },
      ],
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    sessionStorage.clear();

    // Silencia console.error para mantener la salida de tests limpia (algunas pruebas provocan errores esperados)
    spyOn(console, 'error').and.callFake(() => {});
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('initializeAuthentication()', () => {
    it('should set unauthenticated when session endpoint returns unauthenticated', async () => {
      const promise = service.initializeAuthentication();

      const req = httpMock.expectOne(`${environment.apiUrl}/auth/session`);
      expect(req.request.method).toBe('GET');

      req.flush({
        authenticated: false,
        userId: '',
        username: null,
        rolesAndPermissions: [],
        isAdmin: false,
      });

      await promise;

      expect(await firstValueFrom(service.isAuthenticated$)).toBeFalse();
      expect(await firstValueFrom(service.isDoneLoading$)).toBeTrue();
      expect(service.getCurrentAuthenticatedUser()).toBeNull();
    });

    it('should set authenticated, load user and navigate after login when callback', async () => {
      const mockUser: User = {
        id: 123,
        username: 'test',
        email: 't@test.com',
      } as any;
      userServiceSpy.getById.and.returnValue(of(mockUser));
      sessionStorage.setItem('postLoginRedirectUrl', '/after-login');

      spyOn<any>(service, 'isLoginCallback').and.returnValue(true);

      const promise = service.initializeAuthentication();

      const req = httpMock.expectOne(`${environment.apiUrl}/auth/session`);
      expect(req.request.method).toBe('GET');

      req.flush({
        authenticated: true,
        userId: '123',
        username: 'test',
        rolesAndPermissions: [],
        isAdmin: false,
      });

      await promise;

      expect(await firstValueFrom(service.isAuthenticated$)).toBeTrue();
      expect(await firstValueFrom(service.isDoneLoading$)).toBeTrue();
      expect(service.getCurrentAuthenticatedUser()).toEqual(mockUser);
      expect(routerSpy.navigateByUrl).toHaveBeenCalledWith('/after-login', {
        replaceUrl: true,
      });
    });

    it('should handle user fetch failure gracefully', async () => {
      userServiceSpy.getById.and.returnValue(
        throwError(() => new Error('fetch error')),
      );

      const promise = service.initializeAuthentication();

      const req = httpMock.expectOne(`${environment.apiUrl}/auth/session`);
      expect(req.request.method).toBe('GET');

      req.flush({
        authenticated: true,
        userId: '456',
        username: 'fail',
        rolesAndPermissions: [],
        isAdmin: false,
      });

      await promise;

      expect(await firstValueFrom(service.isAuthenticated$)).toBeTrue();
      expect(service.getCurrentAuthenticatedUser()).toBeNull();
      expect(await firstValueFrom(service.isDoneLoading$)).toBeTrue();
    });

    it('should treat 401 as unauthenticated and not throw', async () => {
      const promise = service.initializeAuthentication();

      const req = httpMock.expectOne(`${environment.apiUrl}/auth/session`);
      expect(req.request.method).toBe('GET');

      req.flush({}, { status: 401, statusText: 'Unauthorized' });

      await promise;

      expect(await firstValueFrom(service.isAuthenticated$)).toBeFalse();
      expect(await firstValueFrom(service.isDoneLoading$)).toBeTrue();
    });
  });

  describe('enforceAuthentication()', () => {
    it('should call startLoginFlow when not authenticated', async () => {
      spyOn<any>(service, 'startLoginFlow');

      const result = await firstValueFrom(
        service.enforceAuthentication('/redirect-to'),
      );

      expect(result).toBeFalse();
      expect((service as any).startLoginFlow).toHaveBeenCalledWith(
        '/redirect-to',
      );
    });

    it('should not call startLoginFlow when authenticated', async () => {
      spyOn<any>(service, 'startLoginFlow');
      (service as any).isAuthenticatedSubject$.next(true);

      const result = await firstValueFrom(
        service.enforceAuthentication('/redirect-to'),
      );

      expect(result).toBeTrue();
      expect((service as any).startLoginFlow).not.toHaveBeenCalled();
    });
  });

  describe('logout()', () => {
    it('should clear auth state, remove postLoginRedirectUrl and redirect to logout', async () => {
      const redirectSpy = spyOn<any>(service, 'redirectTo').and.callFake(
        () => {},
      );

      try {
        // establecer estado de prueba
        (service as any).isAuthenticatedSubject$.next(true);
        (service as any).sessionSubject$.next({
          authenticated: true,
          userId: '789',
          username: 'u',
          rolesAndPermissions: [],
          isAdmin: false,
        });
        (service as any).userSubject$.next({ id: 789, username: 'u' } as any);
        sessionStorage.setItem('postLoginRedirectUrl', '/somewhere');

        service.logout();

        expect(await firstValueFrom(service.isAuthenticated$)).toBeFalse();
        expect(service.getCurrentAuthenticatedUser()).toBeNull();
        expect(sessionStorage.getItem('postLoginRedirectUrl')).toBeNull();
        expect(redirectSpy).toHaveBeenCalledWith(
          `${environment.apiUrl}/logout`,
        );
      } finally {
        redirectSpy.and.callThrough();
      }
    });

    it('should still redirect even if no postLoginRedirectUrl exists', () => {
      const redirectSpy = spyOn<any>(service, 'redirectTo').and.callFake(
        () => {},
      );

      try {
        sessionStorage.removeItem('postLoginRedirectUrl');

        service.logout();

        expect(redirectSpy).toHaveBeenCalledWith(
          `${environment.apiUrl}/logout`,
        );
      } finally {
        redirectSpy.and.callThrough();
      }
    });
  });

  describe('getUserId()', () => {
    it('should return null when there is no session', () => {
      (service as any).sessionSubject$.next(null);

      expect(service.getUserId()).toBeNull();
    });

    it('should return null when userId is empty or missing', () => {
      (service as any).sessionSubject$.next({
        authenticated: true,
        userId: '',
        username: null,
        rolesAndPermissions: [],
        isAdmin: false,
      });

      expect(service.getUserId()).toBeNull();
    });

    it('should parse numeric userId string to number', () => {
      (service as any).sessionSubject$.next({
        authenticated: true,
        userId: '123',
        username: 'u',
        rolesAndPermissions: [],
        isAdmin: false,
      });

      expect(service.getUserId()).toBe(123);
    });

    it('should return null for non-numeric userId string', () => {
      (service as any).sessionSubject$.next({
        authenticated: true,
        userId: 'abc',
        username: 'u',
        rolesAndPermissions: [],
        isAdmin: false,
      });

      expect(service.getUserId()).toBeNull();
    });

    it('should return 0 for userId "0"', () => {
      (service as any).sessionSubject$.next({
        authenticated: true,
        userId: '0',
        username: 'u',
        rolesAndPermissions: [],
        isAdmin: false,
      });

      expect(service.getUserId()).toBe(0);
    });
  });

  describe('getUsername()', () => {
    it('should return null when there is no session', () => {
      (service as any).sessionSubject$.next(null);

      expect(service.getUsername()).toBeNull();
    });

    it('should return null when username is null', () => {
      (service as any).sessionSubject$.next({
        authenticated: true,
        userId: '1',
        username: null,
        rolesAndPermissions: [],
        isAdmin: false,
      });

      expect(service.getUsername()).toBeNull();
    });

    it('should return username when present', () => {
      (service as any).sessionSubject$.next({
        authenticated: true,
        userId: '1',
        username: 'alice',
        rolesAndPermissions: [],
        isAdmin: false,
      });

      expect(service.getUsername()).toBe('alice');
    });

    it('should return empty string when username is empty string', () => {
      (service as any).sessionSubject$.next({
        authenticated: true,
        userId: '1',
        username: '',
        rolesAndPermissions: [],
        isAdmin: false,
      });

      expect(service.getUsername()).toBe('');
    });
  });

  describe('isAdmin()', () => {
    it('should return false when there is no session', () => {
      (service as any).sessionSubject$.next(null);

      expect(service.isAdmin()).toBeFalse();
    });

    it('should return false when isAdmin is false', () => {
      (service as any).sessionSubject$.next({
        authenticated: true,
        userId: '1',
        username: 'bob',
        rolesAndPermissions: [],
        isAdmin: false,
      });

      expect(service.isAdmin()).toBeFalse();
    });

    it('should return true when isAdmin is true', () => {
      (service as any).sessionSubject$.next({
        authenticated: true,
        userId: '2',
        username: 'admin',
        rolesAndPermissions: [],
        isAdmin: true,
      });

      expect(service.isAdmin()).toBeTrue();
    });

    it('should return false when isAdmin is missing', () => {
      // @ts-ignore: simular campo faltante
      (service as any).sessionSubject$.next({
        authenticated: true,
        userId: '3',
        username: 'noadmin',
        rolesAndPermissions: [],
      });

      expect(service.isAdmin()).toBeFalse();
    });
  });

  describe('getRolesAndPermissions()', () => {
    it('should return empty set when there is no session', () => {
      (service as any).sessionSubject$.next(null);

      const res = service.getRolesAndPermissions();
      expect(res instanceof Set).toBeTrue();
      expect(res.size).toBe(0);
    });

    it('should return empty set when roles are empty or missing', () => {
      (service as any).sessionSubject$.next({
        authenticated: true,
        userId: '1',
        username: 'u',
        rolesAndPermissions: [],
        isAdmin: false,
      });

      const res = service.getRolesAndPermissions();
      expect(res.size).toBe(0);
    });

    it('should return set with roles and permissions', () => {
      (service as any).sessionSubject$.next({
        authenticated: true,
        userId: '2',
        username: 'u',
        rolesAndPermissions: ['ROLE_USER', 'PERM_READ'],
        isAdmin: false,
      });

      const res = service.getRolesAndPermissions();
      expect(res.has('ROLE_USER')).toBeTrue();
      expect(res.has('PERM_READ')).toBeTrue();
      expect(res.size).toBe(2);
    });

    it('should dedupe duplicate roles', () => {
      (service as any).sessionSubject$.next({
        authenticated: true,
        userId: '3',
        username: 'u',
        rolesAndPermissions: ['a', 'a', 'b'],
        isAdmin: false,
      });

      const res = service.getRolesAndPermissions();
      expect(res.size).toBe(2);
      expect(res.has('a')).toBeTrue();
      expect(res.has('b')).toBeTrue();
    });
  });

  describe('isReadyAndAuthenticated$', () => {
    it('should be false initially and true only when both authenticated and done loading are true', () => {
      const emitted: boolean[] = [];
      const sub = service.isReadyAndAuthenticated$.subscribe((v) =>
        emitted.push(v),
      );

      try {
        // inicial (false && false)
        expect(emitted.at(-1)).toBeFalse();

        // autenticado true, 'isDoneLoading' false -> sigue siendo false
        (service as any).isAuthenticatedSubject$.next(true);
        expect(emitted.at(-1)).toBeFalse();

        // ahora 'isDoneLoading' true -> debería volverse true
        (service as any).isDoneLoadingSubject$.next(true);
        expect(emitted.at(-1)).toBeTrue();
      } finally {
        sub.unsubscribe();
        // restaurar valores por defecto
        (service as any).isAuthenticatedSubject$.next(false);
        (service as any).isDoneLoadingSubject$.next(false);
      }
    });

    it('should remain false when authenticated is false and done loading is true', () => {
      const emitted: boolean[] = [];
      const sub = service.isReadyAndAuthenticated$.subscribe((v) =>
        emitted.push(v),
      );

      try {
        (service as any).isAuthenticatedSubject$.next(false);
        (service as any).isDoneLoadingSubject$.next(true);

        expect(emitted.at(-1)).toBeFalse();
      } finally {
        sub.unsubscribe();
        (service as any).isDoneLoadingSubject$.next(false);
      }
    });
  });

  describe('navigateAfterLogin()', () => {
    it('should navigate to stored postLoginRedirectUrl and remove it', () => {
      routerSpy.navigateByUrl.calls.reset();
      sessionStorage.setItem('postLoginRedirectUrl', '/after-login-url');

      (service as any).navigateAfterLogin();

      expect(routerSpy.navigateByUrl).toHaveBeenCalledWith('/after-login-url', {
        replaceUrl: true,
      });
      expect(sessionStorage.getItem('postLoginRedirectUrl')).toBeNull();
    });

    it('should navigate to /dashboard when no redirect stored', () => {
      routerSpy.navigateByUrl.calls.reset();
      sessionStorage.removeItem('postLoginRedirectUrl');

      (service as any).navigateAfterLogin();

      expect(routerSpy.navigateByUrl).toHaveBeenCalledWith('/dashboard', {
        replaceUrl: true,
      });
    });
  });

  describe('fetchAndStoreCurrentAuthenticatedUser()', () => {
    it('should set user to null and warn when no userId', async () => {
      const warnSpy = spyOn(console, 'warn').and.callFake(() => {});

      // sin sesión / sin userId
      (service as any).sessionSubject$.next(null);

      await (service as any).fetchAndStoreCurrentAuthenticatedUser();

      expect(service.getCurrentAuthenticatedUser()).toBeNull();
      expect(warnSpy).toHaveBeenCalled();
    });

    it('should store fetched user when getById succeeds', async () => {
      const mockUser: User = { id: 321, username: 'fetched' } as any;
      (service as any).sessionSubject$.next({
        authenticated: true,
        userId: '321',
        username: 'u',
        rolesAndPermissions: [],
        isAdmin: false,
      });

      userServiceSpy.getById.and.returnValue(of(mockUser));

      await (service as any).fetchAndStoreCurrentAuthenticatedUser();

      expect(service.getCurrentAuthenticatedUser()).toEqual(mockUser);
    });

    it('should set user to null and call console.error when getById errors', async () => {
      (console.error as jasmine.Spy).calls.reset();

      (service as any).sessionSubject$.next({
        authenticated: true,
        userId: '456',
        username: 'u',
        rolesAndPermissions: [],
        isAdmin: false,
      });

      userServiceSpy.getById.and.returnValue(
        throwError(() => new Error('server error')),
      );

      await (service as any).fetchAndStoreCurrentAuthenticatedUser();

      expect(service.getCurrentAuthenticatedUser()).toBeNull();
      expect((console.error as jasmine.Spy).calls.any()).toBeTrue();
    });
  });
});

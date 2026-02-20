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
    spyOn(console, 'error').and.callFake(() => {
      /* noop */
    });
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('Debe ser instanciado', () => {
    expect(service).toBeTruthy();
  });

  describe('initializeAuthentication()', () => {
    it('Debe establecer no autenticado cuando el endpoint de sesión retorna no autenticado', async () => {
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

    it('Debe establecer autenticado, cargar usuario y navegar tras login cuando es callback', async () => {
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

    it('Debe manejar la falla de carga de usuario correctamente', async () => {
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

    it('Debe tratar 401 como no autenticado y no lanzar error', async () => {
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
    it('Debe llamar a startLoginFlow cuando no está autenticado', async () => {
      spyOn<any>(service, 'startLoginFlow');

      const result = await firstValueFrom(
        service.enforceAuthentication('/redirect-to'),
      );

      expect(result).toBeFalse();
      expect((service as any).startLoginFlow).toHaveBeenCalledWith(
        '/redirect-to',
      );
    });

    it('Debe no llamar a startLoginFlow cuando está autenticado', async () => {
      spyOn<any>(service, 'startLoginFlow');
      (service as any)._isAuthenticated.set(true);

      const result = await firstValueFrom(
        service.enforceAuthentication('/redirect-to'),
      );

      expect(result).toBeTrue();
      expect((service as any).startLoginFlow).not.toHaveBeenCalled();
    });
  });

  describe('logout()', () => {
    it('Debe limpiar estado de autenticación, eliminar postLoginRedirectUrl y redirigir a logout', async () => {
      const redirectSpy = spyOn<any>(service, 'redirectTo').and.callFake(
        () => {},
      );

      try {
        // establecer estado de prueba
        (service as any)._isAuthenticated.set(true);
        (service as any)._session.set({
          authenticated: true,
          userId: '789',
          username: 'u',
          rolesAndPermissions: [],
          isAdmin: false,
        });
        (service as any)._user.set({ id: 789, username: 'u' } as any);
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

    it('Debe redirigir aún si no existe postLoginRedirectUrl', () => {
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
    it('Debe retornar null cuando no hay sesión', () => {
      (service as any)._session.set(null);

      expect(service.getUserId()).toBeNull();
    });

    it('Debe retornar null cuando userId está vacío o falta', () => {
      (service as any)._session.set({
        authenticated: true,
        userId: '',
        username: null,
        rolesAndPermissions: [],
        isAdmin: false,
      });

      expect(service.getUserId()).toBeNull();
    });

    it('Debe parsear userId numérico como string a number', () => {
      (service as any)._session.set({
        authenticated: true,
        userId: '123',
        username: 'u',
        rolesAndPermissions: [],
        isAdmin: false,
      });

      expect(service.getUserId()).toBe(123);
    });

    it('Debe retornar null para userId no numérico', () => {
      (service as any)._session.set({
        authenticated: true,
        userId: 'abc',
        username: 'u',
        rolesAndPermissions: [],
        isAdmin: false,
      });

      expect(service.getUserId()).toBeNull();
    });

    it('Debe retornar 0 para userId "0"', () => {
      (service as any)._session.set({
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
    it('Debe retornar null cuando no hay sesión', () => {
      (service as any)._session.set(null);

      expect(service.getUsername()).toBeNull();
    });

    it('Debe retornar null cuando username es null', () => {
      (service as any)._session.set({
        authenticated: true,
        userId: '1',
        username: null,
        rolesAndPermissions: [],
        isAdmin: false,
      });

      expect(service.getUsername()).toBeNull();
    });

    it('Debe retornar username cuando está presente', () => {
      (service as any)._session.set({
        authenticated: true,
        userId: '1',
        username: 'alice',
        rolesAndPermissions: [],
        isAdmin: false,
      });

      expect(service.getUsername()).toBe('alice');
    });

    it('Debe retornar string vacío cuando username es string vacío', () => {
      (service as any)._session.set({
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
    it('Debe retornar false cuando no hay sesión', () => {
      (service as any)._session.set(null);

      expect(service.isAdmin()).toBeFalse();
    });

    it('Debe retornar false cuando isAdmin es false', () => {
      (service as any)._session.set({
        authenticated: true,
        userId: '1',
        username: 'bob',
        rolesAndPermissions: [],
        isAdmin: false,
      });

      expect(service.isAdmin()).toBeFalse();
    });

    it('Debe retornar true cuando isAdmin es true', () => {
      (service as any)._session.set({
        authenticated: true,
        userId: '2',
        username: 'admin',
        rolesAndPermissions: [],
        isAdmin: true,
      });

      expect(service.isAdmin()).toBeTrue();
    });

    it('Debe retornar false cuando isAdmin no está definido', () => {
      (service as any)._session.set({
        authenticated: true,
        userId: '3',
        username: 'noadmin',
        rolesAndPermissions: [],
      });

      expect(service.isAdmin()).toBeFalse();
    });
  });

  describe('getRolesAndPermissions()', () => {
    it('Debe retornar Set vacío cuando no hay sesión', () => {
      (service as any)._session.set(null);

      const res = service.getRolesAndPermissions();
      expect(res instanceof Set).toBeTrue();
      expect(res.size).toBe(0);
    });

    it('Debe retornar Set vacío cuando los roles están vacíos o faltan', () => {
      (service as any)._session.set({
        authenticated: true,
        userId: '1',
        username: 'u',
        rolesAndPermissions: [],
        isAdmin: false,
      });

      const res = service.getRolesAndPermissions();
      expect(res.size).toBe(0);
    });

    it('Debe retornar Set con roles y permisos', () => {
      (service as any)._session.set({
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

    it('Debe deduplicar roles duplicados', () => {
      (service as any)._session.set({
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
    it('Debe ser false inicialmente y true solo cuando autenticado y carga completada son true', async () => {
      // inicial (false && false)
      expect(
        await firstValueFrom(service.isReadyAndAuthenticated$),
      ).toBeFalse();

      // autenticado true, 'isDoneLoading' false -> sigue siendo false
      (service as any)._isAuthenticated.set(true);
      expect(
        await firstValueFrom(service.isReadyAndAuthenticated$),
      ).toBeFalse();

      // ahora 'isDoneLoading' true -> debería volverse true
      (service as any)._isDoneLoading.set(true);
      expect(await firstValueFrom(service.isReadyAndAuthenticated$)).toBeTrue();

      // restaurar valores por defecto
      (service as any)._isAuthenticated.set(false);
      (service as any)._isDoneLoading.set(false);
    });

    it('Debe permanecer false cuando autenticado es false y carga completada es true', async () => {
      (service as any)._isAuthenticated.set(false);
      (service as any)._isDoneLoading.set(true);

      expect(
        await firstValueFrom(service.isReadyAndAuthenticated$),
      ).toBeFalse();

      (service as any)._isDoneLoading.set(false);
    });
  });

  describe('navigateAfterLogin()', () => {
    it('Debe navegar a postLoginRedirectUrl almacenada y eliminarla', () => {
      routerSpy.navigateByUrl.calls.reset();
      sessionStorage.setItem('postLoginRedirectUrl', '/after-login-url');

      (service as any).navigateAfterLogin();

      expect(routerSpy.navigateByUrl).toHaveBeenCalledWith('/after-login-url', {
        replaceUrl: true,
      });
      expect(sessionStorage.getItem('postLoginRedirectUrl')).toBeNull();
    });

    it('Debe navegar a /dashboard cuando no hay redirección almacenada', () => {
      routerSpy.navigateByUrl.calls.reset();
      sessionStorage.removeItem('postLoginRedirectUrl');

      (service as any).navigateAfterLogin();

      expect(routerSpy.navigateByUrl).toHaveBeenCalledWith('/dashboard', {
        replaceUrl: true,
      });
    });
  });

  describe('fetchAndStoreCurrentAuthenticatedUser()', () => {
    it('Debe establecer usuario como null y advertir cuando no hay userId', async () => {
      const warnSpy = spyOn(console, 'warn').and.callFake(() => {
        /* noop */
      });

      // sin sesión / sin userId
      (service as any)._session.set(null);

      await (service as any).fetchAndStoreCurrentAuthenticatedUser();

      expect(service.getCurrentAuthenticatedUser()).toBeNull();
      expect(warnSpy).toHaveBeenCalled();
    });

    it('Debe almacenar usuario obtenido cuando getById tiene éxito', async () => {
      const mockUser: User = { id: 321, username: 'fetched' } as any;
      (service as any)._session.set({
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

    it('Debe establecer usuario como null y llamar console.error cuando getById falla', async () => {
      (console.error as jasmine.Spy).calls.reset();

      (service as any)._session.set({
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

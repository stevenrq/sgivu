import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { of } from 'rxjs';
import { provideHttpClient } from '@angular/common/http';

import { AuthService } from './auth.service';
import { UserService } from '../../users/services/user.service';
import { User } from '../../users/models/user.model';
import { environment } from '../../../../environments/environment';

interface AuthServiceState {
  isAuthenticatedSubject$: { getValue(): boolean; next(value: boolean): void };
  isDoneLoadingSubject$: { getValue(): boolean };
  userSubject$: { getValue(): User | null; next(value: User | null): void };
}

describe('AuthService', () => {
  let service: AuthService;
  let serviceState: AuthServiceState;
  let userService: jasmine.SpyObj<UserService>;
  let router: jasmine.SpyObj<Router>;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    userService = jasmine.createSpyObj<UserService>('UserService', ['getById']);
    router = jasmine.createSpyObj<Router>('Router', ['navigateByUrl']);

    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: UserService, useValue: userService },
        { provide: Router, useValue: router },
      ],
    });

    service = TestBed.inject(AuthService);
    serviceState = service as unknown as AuthServiceState;
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    sessionStorage.clear();
  });

  it('inicializa autenticación, marca estados y obtiene el usuario', async () => {
    const user: User = {
      id: 99,
      nationalId: 123,
      firstName: 'Ada',
      lastName: 'Lovelace',
      address: { street: 'Main', number: '1', city: 'Bogotá' },
      phoneNumber: 123,
      username: 'ada',
      email: 'ada@example.com',
      roles: new Set(),
      enabled: true,
      accountNonExpired: true,
      accountNonLocked: true,
      credentialsNonExpired: true,
      admin: false,
    };
    userService.getById.and.returnValue(of(user));

    const initPromise = service.initializeAuthentication();
    const req = httpMock.expectOne(`${environment.apiUrl}/auth/session`);
    req.flush({
      authenticated: true,
      userId: '99',
      username: 'ada',
      rolesAndPermissions: [],
      isAdmin: false,
    });

    await initPromise;

    expect(service.getCurrentAuthenticatedUser()).toEqual(user);
    expect(serviceState.isAuthenticatedSubject$.getValue()).toBeTrue();
    expect(serviceState.isDoneLoadingSubject$.getValue()).toBeTrue();
  });

  it('inicia el flujo de login guardando la ruta de retorno', () => {
    const redirectSpy = spyOn(service as any, 'redirectTo');

    service.startLoginFlow('/secure');

    expect(sessionStorage.getItem('postLoginRedirectUrl')).toBe('/secure');
    expect(redirectSpy).toHaveBeenCalledWith(
      `${environment.apiUrl}/oauth2/authorization/sgivu-gateway`,
    );
  });

  it('limpia el estado y redirige al logout del gateway', () => {
    const redirectSpy = spyOn(service as any, 'redirectTo');
    serviceState.isAuthenticatedSubject$.next(true);
    serviceState.userSubject$.next({
      id: 1,
      nationalId: 1,
      firstName: 'Test',
      lastName: 'User',
      address: { street: 'Main', number: '1', city: 'Bogotá' },
      phoneNumber: 123,
      username: 'test',
      email: 't@example.com',
      roles: new Set(),
      enabled: true,
      accountNonExpired: true,
      accountNonLocked: true,
      credentialsNonExpired: true,
      admin: false,
    });
    sessionStorage.setItem('postLoginRedirectUrl', '/dashboard');

    service.logout();

    expect(serviceState.isAuthenticatedSubject$.getValue()).toBeFalse();
    expect(serviceState.userSubject$.getValue()).toBeNull();
    expect(sessionStorage.getItem('postLoginRedirectUrl')).toBeNull();
    expect(redirectSpy).toHaveBeenCalledWith(`${environment.apiUrl}/logout`);
  });

  it('enforceAuthentication dispara login cuando no está autenticado', (done) => {
    spyOn(service, 'startLoginFlow');

    service.enforceAuthentication('/private').subscribe(() => {
      expect(service.startLoginFlow).toHaveBeenCalledWith('/private');
      done();
    });
  });
});

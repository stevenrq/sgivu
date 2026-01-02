import { TestBed } from '@angular/core/testing';
import {
  HttpInterceptorFn,
  HttpRequest,
  HttpResponse,
} from '@angular/common/http';
import { of } from 'rxjs';

import { defaultOAuthInterceptor } from './default-oauth.interceptor';
import { environment } from '../../../../environments/environment';
import { AuthService } from '../services/auth.service';

describe('defaultOauthInterceptor', () => {
  const interceptor: HttpInterceptorFn = (req, next) =>
    TestBed.runInInjectionContext(() => defaultOAuthInterceptor(req, next));

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        {
          provide: AuthService,
          useValue: { startLoginFlow: jasmine.createSpy('startLoginFlow') },
        },
      ],
    });
  });

  it('agrega withCredentials en llamadas al gateway', (done) => {
    const req = new HttpRequest('GET', `${environment.apiUrl}/v1/users`);

    interceptor(req, (nextReq) => {
      expect(nextReq.withCredentials).toBeTrue();
      return of(new HttpResponse({ status: 200 }));
    }).subscribe({
      complete: () => done(),
    });
  });
});

import { TestBed } from '@angular/core/testing';
import { TemplateRef, ViewContainerRef } from '@angular/core';
import { of } from 'rxjs';
import { PermissionService } from '../../features/auth/services/permission.service';
import { HasPermissionDirective } from './has-permission.directive';

describe('HasPermissionDirective', () => {
  it('should create an instance', () => {
    TestBed.configureTestingModule({
      providers: [
        HasPermissionDirective,
        { provide: TemplateRef, useValue: {} },
        {
          provide: ViewContainerRef,
          useValue: {
            createEmbeddedView: jasmine.createSpy('createEmbeddedView'),
            clear: jasmine.createSpy('clear'),
          },
        },
        {
          provide: PermissionService,
          useValue: {
            hasPermission: () => of(true),
          },
        },
      ],
    });

    const directive = TestBed.inject(HasPermissionDirective);
    expect(directive).toBeTruthy();
  });
});

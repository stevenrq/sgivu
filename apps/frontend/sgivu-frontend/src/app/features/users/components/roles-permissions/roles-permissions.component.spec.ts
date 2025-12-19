import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RolesPermissionsComponent } from './roles-permissions.component';

interface BootstrapModalMock {
  show(): void;
  hide(): void;
}

declare global {
  interface Window {
    bootstrap: {
      Modal: new (element: Element) => BootstrapModalMock;
    };
  }
}

describe('RolesPermissionsComponent', () => {
  let component: RolesPermissionsComponent;
  let fixture: ComponentFixture<RolesPermissionsComponent>;

  beforeEach(async () => {
    (globalThis as unknown as Window).bootstrap = {
      Modal: class {
        // eslint-disable-next-line @typescript-eslint/no-empty-function
        constructor() {}
        // eslint-disable-next-line @typescript-eslint/no-empty-function
        show() {}
        // eslint-disable-next-line @typescript-eslint/no-empty-function
        hide() {}
      },
    };
    await TestBed.configureTestingModule({
      imports: [RolesPermissionsComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(RolesPermissionsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

import { AsyncPipe, isPlatformBrowser } from '@angular/common';
import {
  AfterViewInit,
  Component,
  ElementRef,
  OnInit,
  PLATFORM_ID,
  inject,
  viewChild,
  ChangeDetectionStrategy,
} from '@angular/core';
import { RouterModule } from '@angular/router';
import { Observable, of } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import Collapse from 'bootstrap/js/dist/collapse';
import { AuthService } from '../../../features/auth/services/auth.service';
import { UserService } from '../../../features/users/services/user.service';
import { ThemeService } from '../../services/theme.service';

@Component({
  selector: 'app-navbar',
  imports: [RouterModule, AsyncPipe],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.css',
  host: {
    '(window:resize)': 'onWindowResize()',
  },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NavbarComponent implements OnInit, AfterViewInit {
  readonly authService = inject(AuthService);
  private readonly userService = inject(UserService);
  private readonly themeService = inject(ThemeService);
  private readonly platformId = inject(PLATFORM_ID);

  protected user$!: Observable<
    import('../../../features/users/models/user.model').User | null
  >;

  protected readonly isReadyAndAuthenticated =
    this.authService.isReadyAndAuthenticated;
  protected readonly activeTheme = this.themeService.activeTheme;

  private readonly desktopBreakpoint = 992;
  protected isMobileView = false;
  protected isMenuOpen = false;

  private readonly navbarCollapse =
    viewChild<ElementRef<HTMLDivElement>>('navbarCollapse');
  private collapseInstance?: Collapse;

  ngOnInit(): void {
    this.updateResponsiveState();
    this.user$ = this.authService.currentAuthenticatedUser$.pipe(
      switchMap((authenticatedUser) => {
        if (authenticatedUser?.id) {
          return this.userService.getById(authenticatedUser.id);
        }
        return of(null);
      }),
    );
  }

  ngAfterViewInit(): void {
    this.initializeCollapseInstance();
  }

  onWindowResize(): void {
    this.updateResponsiveState();
  }

  login() {
    this.authService.startLoginFlow();
  }

  logout(): void {
    this.authService.logout();
    this.handleNavigation();
  }

  toggleTheme(): void {
    this.themeService.toggleTheme();
  }

  toggleMenu(): void {
    if (this.isMobileView) {
      if (!this.collapseInstance) {
        this.initializeCollapseInstance();
      }
      if (this.isMenuOpen) {
        this.collapseInstance?.hide();
      } else {
        this.collapseInstance?.show();
      }
    }
  }

  handleNavigation(): void {
    if (this.isMobileView) {
      if (!this.collapseInstance) {
        this.initializeCollapseInstance();
      }
      this.collapseInstance?.hide();
    }
  }

  private initializeCollapseInstance(): void {
    const collapseRef = this.navbarCollapse();
    if (!collapseRef || !isPlatformBrowser(this.platformId)) {
      return;
    }

    this.collapseInstance = Collapse.getOrCreateInstance(
      collapseRef.nativeElement,
      {
        toggle: false,
      },
    );

    collapseRef.nativeElement.addEventListener('shown.bs.collapse', () => {
      this.isMenuOpen = true;
    });

    collapseRef.nativeElement.addEventListener('hidden.bs.collapse', () => {
      this.isMenuOpen = false;
    });
  }

  private updateResponsiveState(): void {
    if (!isPlatformBrowser(this.platformId)) {
      this.isMobileView = false;
      return;
    }

    this.isMobileView = window.innerWidth < this.desktopBreakpoint;

    if (this.isMobileView) {
      this.collapseInstance?.hide();
    } else {
      this.showDesktopMenu();
    }
    this.isMenuOpen = false;
  }

  private showDesktopMenu(): void {
    if (this.collapseInstance) {
      this.collapseInstance.show();
      return;
    }

    const element = this.navbarCollapse()?.nativeElement;
    if (element && !element.classList.contains('show')) {
      element.classList.add('show');
    }
  }
}

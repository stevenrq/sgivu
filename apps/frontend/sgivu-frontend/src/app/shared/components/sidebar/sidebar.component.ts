import { isPlatformBrowser } from '@angular/common';
import {
  Component,
  OnInit,
  PLATFORM_ID,
  inject,
  signal,
  ChangeDetectionStrategy,
} from '@angular/core';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-sidebar',
  imports: [RouterModule],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.css',
  host: {
    '(window:resize)': 'onWindowResize()',
  },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SidebarComponent implements OnInit {
  private readonly platformId = inject(PLATFORM_ID);

  private readonly desktopBreakpoint = 992;
  readonly isMobileView = signal(false);
  readonly isSidebarOpen = signal(true);

  ngOnInit(): void {
    this.updateResponsiveState();
  }

  onWindowResize(): void {
    this.updateResponsiveState();
  }

  toggleSidebar(): void {
    if (!this.isMobileView()) {
      return;
    }
    this.isSidebarOpen.update((open) => !open);
  }

  closeSidebar(): void {
    if (!this.isMobileView()) {
      return;
    }
    this.isSidebarOpen.set(false);
  }

  handleNavigation(): void {
    this.closeSidebar();
  }

  private updateResponsiveState(): void {
    if (!isPlatformBrowser(this.platformId)) {
      this.isMobileView.set(false);
      this.isSidebarOpen.set(true);
      return;
    }

    const wasMobileView = this.isMobileView();
    const mobileView = window.innerWidth < this.desktopBreakpoint;
    this.isMobileView.set(mobileView);

    if (mobileView) {
      if (!wasMobileView) {
        this.isSidebarOpen.set(false);
      }
    } else {
      this.isSidebarOpen.set(true);
    }
  }
}

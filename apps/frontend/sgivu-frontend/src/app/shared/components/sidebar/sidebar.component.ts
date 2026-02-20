import { isPlatformBrowser } from '@angular/common';
import {
  Component,
  OnInit,
  PLATFORM_ID,
  inject,
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
  isMobileView = false;
  isSidebarOpen = true;

  ngOnInit(): void {
    this.updateResponsiveState();
  }

  onWindowResize(): void {
    this.updateResponsiveState();
  }

  toggleSidebar(): void {
    if (!this.isMobileView) {
      return;
    }
    this.isSidebarOpen = !this.isSidebarOpen;
  }

  closeSidebar(): void {
    if (!this.isMobileView) {
      return;
    }
    this.isSidebarOpen = false;
  }

  handleNavigation(): void {
    this.closeSidebar();
  }

  private updateResponsiveState(): void {
    if (!isPlatformBrowser(this.platformId)) {
      this.isMobileView = false;
      this.isSidebarOpen = true;
      return;
    }

    const wasMobileView = this.isMobileView;
    const mobileView = window.innerWidth < this.desktopBreakpoint;
    this.isMobileView = mobileView;

    if (mobileView) {
      if (!wasMobileView) {
        this.isSidebarOpen = false;
      }
    } else {
      this.isSidebarOpen = true;
    }
  }
}

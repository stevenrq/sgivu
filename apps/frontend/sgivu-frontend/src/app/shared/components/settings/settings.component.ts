import {
  Component,
  computed,
  inject,
  ChangeDetectionStrategy,
} from '@angular/core';
import { AuthService } from '../../../features/auth/services/auth.service';
import { ThemePreference, ThemeService } from '../../services/theme.service';

@Component({
  selector: 'app-settings',
  imports: [],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SettingsComponent {
  private readonly authService = inject(AuthService);
  private readonly themeService = inject(ThemeService);

  protected readonly currentUser = this.authService.currentAuthenticatedUser;
  protected readonly selectedTheme = computed(() =>
    this.themeService.preference(),
  );

  onThemePreferenceChange(preference: ThemePreference): void {
    this.themeService.setPreference(preference);
  }
}

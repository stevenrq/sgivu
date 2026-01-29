import { Component, OnInit, inject } from '@angular/core';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-callback',
  imports: [],
  templateUrl: './callback.component.html',
  styleUrl: './callback.component.css',
})
export class CallbackComponent implements OnInit {
  private readonly authService = inject(AuthService);

  ngOnInit(): void {
    this.authService.initializeAuthentication();
  }
}

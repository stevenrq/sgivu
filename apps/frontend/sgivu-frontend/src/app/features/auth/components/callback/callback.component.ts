import { Component, OnInit, inject } from '@angular/core';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-callback',
  imports: [],
  templateUrl: './callback.component.html',
  styleUrl: './callback.component.css',
})
/**
 * Maneja el redireccionamiento del gateway BFF. Completa el flujo de autenticación
 * al volver del proveedor e inicializa la sesión en la aplicación.
 */
export class CallbackComponent implements OnInit {
  private readonly authService = inject(AuthService);

  ngOnInit(): void {
    this.authService.initializeAuthentication();
  }
}

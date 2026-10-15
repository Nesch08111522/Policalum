// src/app/components/login/login.component.ts
import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { LoginResponse } from '../../models/user.model';
import { AuthService } from '../../services/auth.service';
import { TokenService } from '../../services/token.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
})
export class LoginComponent {
  email: string = '';
  password: string = '';
  isLoading: boolean = false;
  error: string | null = null;
  showPassword: boolean = false;

  constructor(
    private authService: AuthService,
    private tokenService: TokenService,
    private router: Router,
  ) {}

  onSubmit(): void {
    if (!this.email || !this.password) {
      this.error = 'Por favor, completa todos los campos';
      return;
    }

    this.isLoading = true;
    this.error = null;

    this.authService.login({ email: this.email, password: this.password }).subscribe({
      next: (response: LoginResponse) => {
        console.log('✅ Login exitoso:', response);
        this.isLoading = false;

        if (response.rol === 'ROLE_ADMIN') {
          this.router.navigate(['/admin']);
        } else if (response.rol === 'ROLE_MULTIFUNCIONAL') {
          this.router.navigate(['/empleado']);
        } else {
          this.router.navigate(['/']);
        }
      },
      error: (err: any) => {
        console.error('❌ Error en login:', err);
        this.isLoading = false;
        this.error = err.error?.message || 'Error al iniciar sesión. Verifica tus credenciales.';
      },
    });
  }

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }
}

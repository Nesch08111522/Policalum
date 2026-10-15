// src/app/components/login/login.component.ts
import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiProblem } from '../../core/api/openapi.types';
import { LoginResponse } from '../../models/user.model';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
})
export class LoginComponent implements OnInit {
  email: string = '';
  password: string = '';
  isLoading: boolean = false;
  error: string | null = null;
  notice: string | null = null;
  showPassword: boolean = false;

  constructor(
    private authService: AuthService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    if (typeof sessionStorage === 'undefined') return;
    this.notice = sessionStorage.getItem('auth_notice');
    sessionStorage.removeItem('auth_notice');
  }

  onSubmit(): void {
    if (!this.email || !this.password) {
      this.error = 'Por favor, completa todos los campos';
      return;
    }

    this.isLoading = true;
    this.error = null;

    this.authService.login({ email: this.email, password: this.password }).subscribe({
      next: (_response: LoginResponse) => {
        this.isLoading = false;
        void this.router.navigate(['/inicio']);
      },
      error: (err: ApiProblem) => {
        this.isLoading = false;
        this.error = err?.message || 'Error al iniciar sesión. Verifica tus credenciales.';
      },
    });
  }

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }
}

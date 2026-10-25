// src/app/services/auth.service.ts
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { LoginRequest, LoginResponse } from '../models/user.model';
import { AvatarService } from '../shared/services/avatar.service';
import { TokenService } from './token.service';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private apiUrl = `${environment.apiUrl}/api/v1/auth`;

  constructor(
    private http: HttpClient,
    private tokenService: TokenService,
    private avatar: AvatarService,
  ) {}

  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, credentials).pipe(
      tap((response) => {
        this.tokenService.setToken(response.token);
        this.tokenService.setRefresh(response.refreshToken);
        this.tokenService.setUser({
          id: response.usuarioId,
          nombre: response.nombre,
          email: response.email,
          rol: response.rol,
          totpConfigurado: response.totpConfigurado,
        });
      }),
    );
  }

  logout(): void {
    this.avatar.limpiar();
    const token = this.tokenService.getToken();
    if (!token) {
      this.tokenService.logout();
      return;
    }
    this.http.post(`${this.apiUrl}/logout`, { refreshToken: this.tokenService.getRefresh() }).subscribe({
      next: () => this.tokenService.logout(),
      error: () => this.tokenService.logout(),
    });
  }

  isAuthenticated(): boolean {
    return this.tokenService.isAuthenticated();
  }

  isAdmin(): boolean {
    return this.tokenService.isAdmin();
  }

  isMultifuncional(): boolean {
    return this.tokenService.isMultifuncional();
  }

  getCurrentUser(): any {
    return this.tokenService.getUser();
  }
}

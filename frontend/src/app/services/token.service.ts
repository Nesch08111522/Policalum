import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { User, UserRole } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class TokenService {
  private readonly tokenKey = 'auth_token';
  private readonly userKey = 'user_data';

  constructor(private readonly router: Router) {}

  setToken(token: string): void {
    if (typeof localStorage !== 'undefined') localStorage.setItem(this.tokenKey, token);
  }

  getToken(): string | null {
    return typeof localStorage === 'undefined' ? null : localStorage.getItem(this.tokenKey);
  }

  setUser(user: User): void {
    if (typeof localStorage !== 'undefined') localStorage.setItem(this.userKey, JSON.stringify(user));
  }

  getUser(): User | null {
    if (typeof localStorage === 'undefined') return null;
    const raw = localStorage.getItem(this.userKey);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as User;
    } catch {
      this.clearStorage();
      return null;
    }
  }

  getRol(): UserRole | null {
    return this.getUser()?.rol ?? null;
  }

  getUserId(): string | null {
    return this.getUser()?.id ?? null;
  }

  isAuthenticated(): boolean {
    return Boolean(this.getToken() && this.getUser());
  }

  isAdmin(): boolean {
    return this.getRol() === 'ROLE_ADMIN';
  }

  isMultifuncional(): boolean {
    return this.getRol() === 'ROLE_MULTIFUNCIONAL';
  }

  logout(): void {
    this.clearStorage();
    void this.router.navigate(['/login']);
  }

  clearStorage(): void {
    if (typeof localStorage === 'undefined') return;
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.userKey);
  }
}

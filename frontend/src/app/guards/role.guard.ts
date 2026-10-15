// src/app/guards/role.guard.ts
import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { TokenService } from '../services/token.service';

@Injectable({
  providedIn: 'root',
})
export class RoleGuard {
  constructor(
    private tokenService: TokenService,
    private router: Router,
  ) {}

  canActivate(route: any): boolean {
    const expectedRoles = route.data.expectedRoles;
    const userRol = this.tokenService.getRol();

    if (!userRol) {
      this.router.navigate(['/login']);
      return false;
    }

    if (expectedRoles.includes(userRol)) {
      return true;
    }

    if (userRol === 'ROLE_ADMIN') {
      this.router.navigate(['/admin']);
    } else if (userRol === 'ROLE_MULTIFUNCIONAL') {
      this.router.navigate(['/empleado']);
    } else {
      this.router.navigate(['/login']);
    }
    return false;
  }
}

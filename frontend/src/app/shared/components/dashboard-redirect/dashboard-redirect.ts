import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-dashboard-redirect',
  standalone: true,
  template: '', // sin vista, redirige automáticamente
})
export class DashboardRedirectComponent implements OnInit {
  constructor(private authService: AuthService, private router: Router) {}

  ngOnInit(): void {
    const user = this.authService.getCurrentUser();
    if (user?.rol === 'ROLE_ADMIN') {
      this.router.navigate(['/admin']);
    } else {
      this.router.navigate(['/empleado']);
    }
  }
}
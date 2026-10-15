import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../services/auth.service';
import { interval, Subscription } from 'rxjs';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './header.html',
  styleUrls: ['./header.css']
})
export class HeaderComponent implements OnInit, OnDestroy {
  currentTime: string = '';
  currentDate: string = '';
  userEmail: string = 'admin@policalum.com';
  userRole: string = 'Administrador';
  isProfileMenuOpen = false;

  private timerSubscription!: Subscription;

  constructor(private authService: AuthService, private router: Router) {}

  ngOnInit(): void {
    const user = this.authService.getCurrentUser();
    if (user) {
      this.userEmail = user.email;
      this.userRole = user.rol;
    }

    this.timerSubscription = interval(1000).subscribe(() => {
      this.updateDateTime();
    });
    this.updateDateTime();
  }

  ngOnDestroy(): void {
    this.timerSubscription?.unsubscribe();
  }

  private updateDateTime(): void {
    const now = new Date();
    this.currentTime = now.toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
    this.currentDate = now.toLocaleDateString('es-ES', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
  }

  toggleProfileMenu(): void {
    this.isProfileMenuOpen = !this.isProfileMenuOpen;
  }

  closeProfileMenu(): void {
    this.isProfileMenuOpen = false;
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
    this.closeProfileMenu();
  }

  goToProfile(): void {
    this.router.navigate(['/perfil']);
    this.closeProfileMenu();
  }
}
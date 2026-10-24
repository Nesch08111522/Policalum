import { CommonModule } from '@angular/common';
import { Component, HostListener, OnInit } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { IconComponent } from '../../../shared/components/icon/icon.component';
import { LayoutService } from '../../../shared/services/layout.service';
import { ThemeService } from '../../../shared/services/theme.service';
import { NAV_SECTIONS, NavItem } from '../../navigation';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterModule, IconComponent],
  templateUrl: './header.html',
  styleUrls: ['./header.css'],
})
export class HeaderComponent implements OnInit {
  query = '';
  results: NavItem[] = [];
  menuOpen = false;
  userName = 'Usuario';
  userEmail = '';
  userRole = '';
  initials = 'P';
  private items: NavItem[] = [];

  constructor(
    readonly theme: ThemeService,
    private readonly layout: LayoutService,
    private readonly auth: AuthService,
    private readonly router: Router,
  ) {}

  ngOnInit(): void {
    const user = this.auth.getCurrentUser();
    const role = user?.rol ?? '';
    this.userEmail = user?.email ?? '';
    this.userName = user?.nombre ?? this.userEmail;
    this.userRole = role === 'ROLE_ADMIN' ? 'Administrador' : 'Multifuncional';
    this.initials = (this.userName || 'P').slice(0, 2).toUpperCase();
    this.items = NAV_SECTIONS.flatMap((section) => section.items).filter((item) => !item.roles || item.roles.includes(role));
  }

  onQuery(event: Event): void {
    this.query = (event.target as HTMLInputElement).value;
    const term = this.query.trim().toLowerCase();
    this.results = term ? this.items.filter((item) => item.label.toLowerCase().includes(term)).slice(0, 6) : [];
  }

  goFirst(): void {
    const target = this.results[0];
    if (!target) return;
    void this.router.navigateByUrl(target.path);
    this.clear();
  }

  clear(): void {
    this.query = '';
    this.results = [];
  }

  toggleMenu(): void {
    if (window.innerWidth <= 900) this.layout.toggleMobile();
    else this.layout.toggleCollapsed();
  }

  ir(ruta: string): void {
    this.menuOpen = false;
    void this.router.navigateByUrl(ruta);
  }

  logout(): void {
    this.auth.logout();
    this.menuOpen = false;
  }

  @HostListener('document:click', ['$event'])
  closeMenus(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (!target.closest('.dropdown')) this.menuOpen = false;
    if (!target.closest('.search-shell')) this.results = [];
  }
}

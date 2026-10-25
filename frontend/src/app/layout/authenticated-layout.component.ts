import { Component, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { BreadcrumbsComponent } from '../shared/components/breadcrumbs/breadcrumbs.component';
import { AvatarService } from '../shared/services/avatar.service';
import { CajaGateService } from '../shared/services/caja-gate.service';
import { LayoutService } from '../shared/services/layout.service';
import { FooterComponent } from './components/footer/footer';
import { HeaderComponent } from './components/header/header';
import { NavbarComponent } from './components/navbar/navbar';
import { SidebarComponent } from './components/sidebar/sidebar';

@Component({
  changeDetection: ChangeDetectionStrategy.Default,
  selector: 'app-authenticated-layout',
  standalone: true,
  imports: [HeaderComponent, NavbarComponent, SidebarComponent, FooterComponent, RouterOutlet, BreadcrumbsComponent],
  template: `
    <div class="app-shell" [class.sidebar-collapsed]="layout.collapsed()" [class.sidebar-open]="layout.mobileOpen()">
      <app-sidebar></app-sidebar>
      <div class="app-main">
        <app-header></app-header>
        <app-navbar></app-navbar>
        <main class="app-content">
          <app-breadcrumbs></app-breadcrumbs>
          <router-outlet></router-outlet>
        </main>
        <app-footer></app-footer>
      </div>
    </div>
  `,
})
export class AuthenticatedLayoutComponent implements OnInit {
  constructor(readonly layout: LayoutService, private readonly caja: CajaGateService, private readonly avatar: AvatarService) {}

  ngOnInit(): void {
    this.caja.vigilar();
    this.avatar.cargar();
  }
}

import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { BreadcrumbsComponent } from '../shared/components/breadcrumbs/breadcrumbs.component';
import { ToastHostComponent } from '../shared/components/toast/toast-host.component';
import { LayoutService } from '../shared/services/layout.service';
import { FooterComponent } from './components/footer/footer';
import { HeaderComponent } from './components/header/header';
import { NavbarComponent } from './components/navbar/navbar';
import { SidebarComponent } from './components/sidebar/sidebar';

@Component({
  selector: 'app-authenticated-layout',
  standalone: true,
  imports: [HeaderComponent, NavbarComponent, SidebarComponent, FooterComponent, RouterOutlet, BreadcrumbsComponent, ToastHostComponent],
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
    <app-toast-host></app-toast-host>
  `,
})
export class AuthenticatedLayoutComponent {
  constructor(readonly layout: LayoutService) {}
}

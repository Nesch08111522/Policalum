import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { HeaderComponent } from './components/header/header';
import { NavbarComponent } from './components/navbar/navbar';
import { SidebarComponent } from './components/sidebar/sidebar';
import { FooterComponent } from './components/footer/footer';

@Component({
  selector: 'app-authenticated-layout',
  standalone: true,
  imports: [HeaderComponent, NavbarComponent, SidebarComponent, FooterComponent, RouterOutlet],
  template: `
    <div class="main-wrapper">
      <app-header></app-header>
      <app-navbar></app-navbar>
      <div class="content-with-sidebar">
        <app-sidebar></app-sidebar>
        <main class="main-content">
          <router-outlet></router-outlet>
        </main>
      </div>
      <app-footer></app-footer>
    </div>
  `,
  styles: [`
    .main-wrapper {
      display: flex;
      flex-direction: column;
      min-height: 100vh;
    }
    .content-with-sidebar {
      display: flex;
      flex: 1;
    }
    .main-content {
      flex: 1;
      padding: 24px;
      background: var(--color-bg-light);
      overflow-y: auto;
    }
  `]
})
export class AuthenticatedLayoutComponent {}
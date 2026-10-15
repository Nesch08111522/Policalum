import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { WORKSPACE_MODULES, WorkspaceModule } from '../../../features/workspace/workspace.config';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './sidebar.html',
  styleUrls: ['./sidebar.css'],
})
export class SidebarComponent implements OnInit {
  modules: WorkspaceModule[] = [];
  isCollapsed = false;
  userRole = 'ROLE_MULTIFUNCIONAL';

  constructor(private readonly router: Router, private readonly authService: AuthService) {}

  ngOnInit(): void {
    this.userRole = this.authService.getCurrentUser()?.rol ?? 'ROLE_MULTIFUNCIONAL';
    this.modules = WORKSPACE_MODULES.filter((module) => !module.roles || module.roles.includes(this.userRole));
  }

  toggleCollapse(): void {
    this.isCollapsed = !this.isCollapsed;
  }

  open(module: WorkspaceModule): void {
    void this.router.navigate(['/modules', module.id]);
  }

  logout(): void {
    this.authService.logout();
  }
}

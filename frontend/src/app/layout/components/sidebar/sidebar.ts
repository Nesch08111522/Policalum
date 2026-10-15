import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../../services/auth.service';
import { IconComponent } from '../../../shared/components/icon/icon.component';
import { LayoutService } from '../../../shared/services/layout.service';
import { NAV_SECTIONS, NavSection } from '../../navigation';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule, IconComponent],
  templateUrl: './sidebar.html',
  styleUrls: ['./sidebar.css'],
})
export class SidebarComponent implements OnInit {
  sections: NavSection[] = [];

  constructor(readonly layout: LayoutService, private readonly auth: AuthService) {}

  ngOnInit(): void {
    const role = this.auth.getCurrentUser()?.rol ?? '';
    this.sections = NAV_SECTIONS
      .map((section) => ({
        ...section,
        items: section.items.filter((item) => !item.roles || item.roles.includes(role)),
      }))
      .filter((section) => section.items.length > 0);
  }
}

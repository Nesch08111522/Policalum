import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterModule } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from '../../../services/auth.service';
import { IconComponent } from '../../../shared/components/icon/icon.component';
import { AvatarService } from '../../../shared/services/avatar.service';
import { LayoutService } from '../../../shared/services/layout.service';
import { MarcaService } from '../../../shared/services/marca.service';
import { NAV_SECTIONS, NavSection } from '../../navigation';

const GROUPS_KEY = 'erp-nav-groups';

@Component({
  changeDetection: ChangeDetectionStrategy.Default,
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule, IconComponent],
  templateUrl: './sidebar.html',
  styleUrls: ['./sidebar.css'],
})
export class SidebarComponent implements OnInit {
  sections: NavSection[] = [];
  userName = 'Usuario';
  userRole = '';
  initials = 'P';
  readonly openGroups = signal<Record<string, boolean>>({});

  private readonly destroyRef = inject(DestroyRef);

  constructor(
    readonly layout: LayoutService,
    readonly marca: MarcaService,
    readonly avatar: AvatarService,
    private readonly auth: AuthService,
    private readonly router: Router,
  ) {}

  ngOnInit(): void {
    const user = this.auth.getCurrentUser();
    const role = user?.rol ?? '';
    this.userName = user?.nombre || user?.email || 'Usuario';
    this.userRole = role === 'ROLE_ADMIN' ? 'Administrador' : role === 'ROLE_MULTIFUNCIONAL' ? 'Multifuncional' : 'Usuario';
    this.initials = this.userName.trim().split(/\s+/).slice(0, 2).map((parte) => parte.charAt(0).toUpperCase()).join('') || 'P';
    this.sections = NAV_SECTIONS
      .map((section) => ({
        ...section,
        items: section.items.filter((item) => !item.roles || item.roles.includes(role)),
      }))
      .filter((section) => section.items.length > 0);
    this.openGroups.set(this.readGroups());
    this.expandActive(this.router.url);
    this.router.events.pipe(
      filter((event) => event instanceof NavigationEnd),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe((event) => {
      this.expandActive((event as NavigationEnd).urlAfterRedirects);
    });
  }

  isOpen(id: string): boolean {
    return this.openGroups()[id] !== false;
  }

  toggleGroup(id: string): void {
    this.openGroups.update((groups) => {
      const next = { ...groups, [id]: groups[id] === false };
      if (typeof localStorage !== 'undefined') localStorage.setItem(GROUPS_KEY, JSON.stringify(next));
      return next;
    });
  }

  logout(): void {
    this.layout.closeMobile();
    this.auth.logout();
  }

  private readGroups(): Record<string, boolean> {
    if (typeof localStorage === 'undefined') return {};
    try {
      const raw = localStorage.getItem(GROUPS_KEY);
      return raw ? (JSON.parse(raw) as Record<string, boolean>) : {};
    } catch {
      return {};
    }
  }

  private expandActive(url: string): void {
    const section = this.sections.find((item) => item.items.some((link) => url === link.path || url.startsWith(`${link.path}/`)));
    if (!section || this.isOpen(section.id)) return;
    this.toggleGroup(section.id);
  }
}

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnDestroy, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { filter, Subscription } from 'rxjs';

interface Crumb { label: string; url?: string; }

@Component({
  changeDetection: ChangeDetectionStrategy.Default,
  selector: 'app-breadcrumbs',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <nav class="breadcrumbs" aria-label="Miga de pan" *ngIf="crumbs().length">
      <a routerLink="/inicio">Inicio</a>
      <ng-container *ngFor="let crumb of crumbs()">
        <span aria-hidden="true">/</span>
        <a *ngIf="crumb.url" [routerLink]="crumb.url">{{ crumb.label }}</a>
        <span *ngIf="!crumb.url">{{ crumb.label }}</span>
      </ng-container>
    </nav>
  `,
})
export class BreadcrumbsComponent implements OnDestroy {
  /** Signal: el arranque es zoneless y NavigationEnd no repinta un arreglo plano. */
  readonly crumbs = signal<Crumb[]>([]);
  private readonly sub: Subscription;

  constructor(private readonly router: Router) {
    this.sub = this.router.events.pipe(filter((event) => event instanceof NavigationEnd)).subscribe(() => this.rebuild());
    this.rebuild();
  }

  ngOnDestroy(): void {
    this.sub.unsubscribe();
  }

  private rebuild(): void {
    const path = this.router.url.split('?')[0];
    if (path === '/inicio') {
      this.crumbs.set([]);
      return;
    }
    let route = this.router.routerState.snapshot.root;
    let section = '';
    let label = '';
    while (route.firstChild) {
      route = route.firstChild;
      section = (route.data['section'] as string) || section;
      label = (route.data['breadcrumb'] as string) || label;
    }
    const crumbs: Crumb[] = [];
    if (section && section !== 'Inicio') crumbs.push({ label: section });
    if (label && label !== section) crumbs.push({ label });
    this.crumbs.set(crumbs);
  }
}

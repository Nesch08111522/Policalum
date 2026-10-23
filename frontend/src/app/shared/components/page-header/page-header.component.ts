import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Breadcrumb } from '../../models/ui.models';
import { IconComponent } from '../icon/icon.component';

@Component({
  changeDetection: ChangeDetectionStrategy.Default,
  selector: 'app-page-header',
  standalone: true,
  imports: [CommonModule, RouterLink, IconComponent],
  template: `
    <header class="page-header">
      <div class="min-w-0">
        <nav *ngIf="breadcrumbs.length" class="breadcrumbs mb-2" aria-label="Miga de pan">
          <ng-container *ngFor="let crumb of breadcrumbs; let last = last">
            <a *ngIf="crumb.link && !last" [routerLink]="crumb.link">{{ crumb.label }}</a>
            <span *ngIf="!crumb.link || last">{{ crumb.label }}</span>
            <span *ngIf="!last" aria-hidden="true">/</span>
          </ng-container>
        </nav>
        <h1>{{ title }}</h1>
        <p *ngIf="subtitle">{{ subtitle }}</p>
      </div>
      <button *ngIf="actionLabel" type="button" class="btn btn-primary" (click)="action.emit()">
        <app-icon name="plus"></app-icon>
        {{ actionLabel }}
      </button>
    </header>
  `,
})
export class PageHeaderComponent {
  @Input() title = '';
  @Input() subtitle = '';
  @Input() breadcrumbs: Breadcrumb[] = [];
  @Input() actionLabel = '';
  @Output() action = new EventEmitter<void>();
}

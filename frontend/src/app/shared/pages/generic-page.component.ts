import { Component, Input } from '@angular/core';
import { RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-generic-page',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="page-container">
      <div class="page-header">
        <h1 class="page-title">
          <i class="fas {{ icon }}" *ngIf="icon"></i> {{ title }}
        </h1>
        <p class="page-description" *ngIf="description">{{ description }}</p>
      </div>
      <div class="page-content">
        <ng-content></ng-content>
      </div>
    </div>
  `,
  styles: [`
    .page-container {
      padding: 1.5rem;
    }
    .page-header {
      margin-bottom: 2rem;
    }
    .page-title {
      font-size: 1.8rem;
      font-weight: 700;
      color: var(--color-logo-bright);
      margin: 0 0 0.5rem 0;
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }
    .page-title i {
      color: var(--color-cyan-light);
      font-size: 1.6rem;
    }
    .page-description {
      color: var(--color-bg-gray);
      font-size: 0.95rem;
      margin: 0;
    }
    .page-content {
      background: var(--color-bg-card, #232538);
      border-radius: 12px;
      padding: 2rem;
      box-shadow: 0 4px 16px rgba(0,0,0,0.3);
      border: 1px solid var(--color-border);
    }
  `]
})
export class GenericPageComponent {
  @Input() title: string = '';
  @Input() icon: string = '';
  @Input() description: string = '';
}
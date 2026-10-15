import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { IconComponent } from '../icon/icon.component';

@Component({
  selector: 'app-stat-card',
  standalone: true,
  imports: [CommonModule, IconComponent],
  template: `
    <article class="card stat-card">
      <div class="stat-icon"><app-icon [name]="icon"></app-icon></div>
      <div class="min-w-0">
        <div class="stat-label">{{ label }}</div>
        <div class="stat-value">{{ value }}</div>
        <div *ngIf="trend" class="text-sm" [class.trend-up]="direction === 'up'" [class.trend-down]="direction === 'down'">{{ trend }}</div>
      </div>
    </article>
  `,
})
export class StatCardComponent {
  @Input() label = '';
  @Input() value: string | number = '—';
  @Input() icon = 'chart';
  @Input() trend = '';
  @Input() direction: 'up' | 'down' | 'flat' = 'flat';
}

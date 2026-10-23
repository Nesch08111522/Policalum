import { CommonModule } from '@angular/common';
import { Component, Input, ChangeDetectionStrategy } from '@angular/core';

@Component({
  changeDetection: ChangeDetectionStrategy.Default,
  selector: 'app-loader',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="skeleton-table" role="status" aria-live="polite">
      <div class="skeleton" *ngFor="let row of rowsArray"></div>
      <span class="sr-only">{{ message }}</span>
    </div>
  `,
})
export class LoaderComponent {
  @Input() rows = 6;
  @Input() message = 'Cargando información';

  get rowsArray(): number[] {
    return Array.from({ length: this.rows }, (_, index) => index);
  }
}

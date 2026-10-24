import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, ChangeDetectionStrategy } from '@angular/core';

@Component({
  changeDetection: ChangeDetectionStrategy.Default,
  selector: 'app-empty-state',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="empty-state">
      <div class="empty-mark" aria-hidden="true">∅</div>
      <strong>{{ title }}</strong>
      <p class="text-muted">{{ message }}</p>
      <button *ngIf="actionLabel" type="button" class="btn btn-primary" (click)="action.emit()">{{ actionLabel }}</button>
    </div>
  `,
})
export class EmptyStateComponent {
  @Input() title = 'Sin resultados';
  @Input() message = 'Ajusta los filtros o crea el primer registro.';
  @Input() actionLabel = '';
  @Output() action = new EventEmitter<void>();
}

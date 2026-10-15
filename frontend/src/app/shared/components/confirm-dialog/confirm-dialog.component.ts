import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ModalComponent } from '../modal/modal.component';

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [ModalComponent],
  template: `
    <app-modal [open]="open" [title]="title" size="sm" (closed)="cancel.emit()">
      <p>{{ message }}</p>
      <div modalFooter>
        <button type="button" class="btn btn-ghost" (click)="cancel.emit()" [disabled]="loading">Cancelar</button>
        <button type="button" class="btn" [class.btn-danger]="danger" [class.btn-primary]="!danger" (click)="confirm.emit()" [disabled]="loading">
          {{ loading ? 'Procesando…' : confirmLabel }}
        </button>
      </div>
    </app-modal>
  `,
})
export class ConfirmDialogComponent {
  @Input() open = false;
  @Input() title = 'Confirmar';
  @Input() message = '¿Deseas continuar?';
  @Input() confirmLabel = 'Confirmar';
  @Input() danger = true;
  @Input() loading = false;
  @Output() confirm = new EventEmitter<void>();
  @Output() cancel = new EventEmitter<void>();
}

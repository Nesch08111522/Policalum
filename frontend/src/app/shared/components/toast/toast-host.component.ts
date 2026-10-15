import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-toast-host',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="toast-host" aria-live="polite" aria-atomic="true">
      <button
        *ngFor="let toast of toasts()"
        type="button"
        class="toast"
        [class.alert-success]="toast.kind === 'success'"
        [class.alert-error]="toast.kind === 'error'"
        [class.alert-warning]="toast.kind === 'warning'"
        [class.alert-info]="toast.kind === 'info'"
        (click)="toastService.dismiss(toast.id)">
        {{ toast.text }}
      </button>
    </div>
  `,
})
export class ToastHostComponent {
  constructor(readonly toastService: ToastService) {}

  toasts() {
    return this.toastService.toasts();
  }
}

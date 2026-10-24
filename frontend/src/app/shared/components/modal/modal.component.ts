import { CommonModule } from '@angular/common';
import { Component, ElementRef, EventEmitter, HostListener, Input, OnChanges, OnDestroy, Output, ViewChild, ChangeDetectionStrategy } from '@angular/core';
import { ModalStackService } from '../../services/modal-stack.service';
import { IconComponent } from '../icon/icon.component';

@Component({
  changeDetection: ChangeDetectionStrategy.Default,
  selector: 'app-modal',
  standalone: true,
  imports: [CommonModule, IconComponent],
  template: `
    <div class="modal-backdrop" *ngIf="open" (click)="requestClose()">
      <div
        #dialog
        class="modal"
        [class.modal-sm]="size === 'sm'"
        [class.modal-lg]="size === 'lg'"
        role="dialog"
        aria-modal="true"
        tabindex="-1"
        [attr.aria-labelledby]="titleId"
        (click)="$event.stopPropagation()">
        <header class="modal-header">
          <h2 [id]="titleId">{{ title }}</h2>
          <button *ngIf="closable" type="button" class="btn btn-ghost btn-sm icon-btn" (click)="requestClose()" aria-label="Cerrar">
            <app-icon name="x"></app-icon>
          </button>
        </header>
        <div class="modal-body"><ng-content></ng-content></div>
        <footer class="modal-footer"><ng-content select="[modalFooter]"></ng-content></footer>
      </div>
    </div>
  `,
})
export class ModalComponent implements OnChanges, OnDestroy {
  @Input() open = false;
  @Input() title = '';
  @Input() size: 'sm' | 'md' | 'lg' = 'md';
  @Input() closable = true;
  @Output() closed = new EventEmitter<void>();
  @ViewChild('dialog') dialog?: ElementRef<HTMLElement>;
  readonly titleId = `modal-${Math.random().toString(36).slice(2, 8)}`;
  private readonly close = () => this.closed.emit();
  private registered = false;

  constructor(private readonly stack: ModalStackService) {}

  ngOnChanges(): void {
    if (this.open && !this.registered) {
      this.stack.register(this.close);
      this.registered = true;
      setTimeout(() => this.dialog?.nativeElement.focus());
    }
    if (!this.open && this.registered) {
      this.stack.unregister(this.close);
      this.registered = false;
    }
  }

  ngOnDestroy(): void {
    if (this.registered) this.stack.unregister(this.close);
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.open && this.closable && this.stack.isTop(this.close)) this.requestClose();
  }

  requestClose(): void {
    if (!this.closable) return;
    this.closed.emit();
  }
}

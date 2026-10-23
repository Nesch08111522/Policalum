import { CommonModule } from '@angular/common';
import { Component, Input, ChangeDetectionStrategy } from '@angular/core';

@Component({
  changeDetection: ChangeDetectionStrategy.Default,
  selector: 'app-form-field',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="field" [class.field-invalid]="!!error">
      <label class="field-label" [attr.for]="inputId">
        {{ label }}
        <span *ngIf="required" class="req" aria-hidden="true">*</span>
      </label>
      <ng-content></ng-content>
      <p *ngIf="error" class="field-error" role="alert">{{ error }}</p>
      <p *ngIf="hint && !error" class="field-hint">{{ hint }}</p>
    </div>
  `,
})
export class FormFieldComponent {
  @Input() label = '';
  @Input() inputId = '';
  @Input() hint = '';
  @Input() error: string | null = null;
  @Input() required = false;
}

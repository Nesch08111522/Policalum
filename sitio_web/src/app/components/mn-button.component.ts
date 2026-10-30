import { Component, Input } from '@angular/core';

@Component({
  selector: 'mn-button',
  standalone: true,
  template: `
    <button class="btn" [class.btn-primary]="variant === 'primary'" [class.btn-sun]="variant === 'outline'" [class.btn-sm]="size === 'sm'" [disabled]="disabled" [attr.type]="type">
      <ng-content></ng-content>
    </button>
  `,
})
export class MnButtonComponent {
  @Input() variant: 'primary' | 'outline' | 'quiet' = 'primary';
  @Input() size: 'md' | 'sm' = 'md';
  @Input() disabled = false;
  @Input() type: 'button' | 'submit' | 'reset' = 'button';
}

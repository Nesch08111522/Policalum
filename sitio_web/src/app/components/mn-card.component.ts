import { Component, Input } from '@angular/core';

@Component({
  selector: 'mn-card',
  standalone: true,
  template: `
    <article class="card" [class.card-raised]="raised">
      @if (title) {
        <header class="card-header"><h2>{{ title }}</h2></header>
      }
      <div class="card-body"><ng-content></ng-content></div>
    </article>
  `,
  styles: `
    .card-header, .card-body { padding: 1rem 1.05rem; }
    .card-header { border-bottom: 1px solid #e7e5e4; }
    h2 { font-size: clamp(1.15rem, 1rem + 0.4vw, 1.4rem); }
  `,
})
export class MnCardComponent {
  @Input() title = '';
  @Input() raised = false;
}
